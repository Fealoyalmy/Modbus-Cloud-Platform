#include <ESP8266WiFi.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "DHT.h" 

#define DHTTYPE DHT11
#define led 2 //发光二极管连接在8266的GPIO2上
#define controller D0

// 网络配置
const char *ssid     = "Mao";//"MERCURY_ED2E"; // 网络的ssid "Mao"
const char *password = "308308308";//"Fhf111083"; //    // wifi密码 308308308
const char *host = "192.168.58.50";   // 修改为Server服务端的IP，即你电脑的IP，确保在同一网络之下。
//String host = "";
WiFiClient client;//声明一个客户端对象，用于与服务器进行连接
const int tcpPort = 8266;//修改为你建立的Server服务端的端口号，此端口号是创建服务器时指定的。

// 定义传感器接口数据初值
static String comdata = "";
static String val = "";
const int analogData = A0;
int inputValue = 0;
DHT dht(analogRead(analogData), DHTTYPE);

// 传感器参数配置
int reg = 1; // 是否需要注册
byte deviceId = 0x01; // 设备ID
int HeartBeat = 0;
int HeartInterval = 5;

void setup()
{
    // 设置传感器接口
    pinMode(analogData, INPUT);
    pinMode(controller, OUTPUT);
    //digitalWrite(analogData,LOW);

    // 设置wifi
    Serial.begin(9600);    //115200
    pinMode(led,OUTPUT);
    delay(10);
    Serial.println();
    Serial.print("Connecting to ");//会通过usb转tll模块发送到电脑，通过ide集成的串口监视器可以获取数据。
    Serial.println(ssid);
    WiFi.begin(ssid, password);//启动
     //在这里检测是否成功连接到目标网络，未连接则阻塞。
    while (WiFi.status() != WL_CONNECTED)
    {
        delay(500);
    }
    //几句提示
    Serial.println("");
    Serial.println("WiFi connected");
    Serial.println("IP address: ");
    Serial.println(WiFi.localIP());
    digitalWrite(controller, LOW);
    //*host = WiFi.localIP();      
}

void loop()
{
      if(reg == 1)
      {
          deviceRegister(deviceId); // 注册     
      }
      
 /******************串口接受数据************************/
//    while (Serial.available() > 0) // 串口收到字符数大于零。
//    {          
//        comdata += char(Serial.read());
//        comdata += char(inputValue - 1);
//        comdata += data;
//        float h = dht.readHumidity();
//    }
 /******************串口打印数据************************/ 
    //if(comdata!="")//如果接受到数据
//    if(data[2] > 0)
//    {
//        client.print(comdata);//向服务器发送数据 
//        client.write(data, 6);
//    }
//    comdata="";//清空数据

 /******************自动判断是否连接并尝试重连************************/ 
    if (client.connected()) //尝试访问目标地址，如果没连接上LED灯灭
        digitalWrite(led, LOW);
    else
        digitalWrite(led, HIGH);

    while (!client.connected())//若未连接到服务端，则客户端进行连接。
    {
        reg = 1;
        if (!client.connect(host, tcpPort))//实际上这一步就在连接服务端，如果连接上，该函数返回true  //WiFi.localIP()
        {
           Serial.println("连接服务器中....");
           delay(500);
        }    
    }

 /******************接收服务器数据************************/ 
    while (client.available()) 
    {
        //Serial.println("接收到服务端数据！");
        byte packet[8]; // 接收数据帧
        int i = 0;
        while (client.available() > 0) // 串口收到字符数大于零。
        {   
            reg = 0;
            packet[i] = char(client.read());
            val += packet[i];
            val += " ";
            i++;
        }
        Serial.println("recieve data from server:" + val);
        val = "";//清空数据
        
        // 阻塞读取服务端下发的指令，若读到-1表示服务器端主动关闭连接，退出本循环，重新尝试连接。
        byte addr = packet[0] & 0xFF; // 地址1字节
        byte func = packet[1] & 0xFF; // 功能码1字节
        int offset = packet[2]*256 + packet[3]; //寄存器2字节
        // 解析数据包
        if(check(packet, 8))
        {
            Serial.println("[client]" + String(deviceId) + "[读到指令] addr = " + String(addr) + ", oprt = " + String(func));
            switch(func)
            {
                case 0x03: // 读保持寄存器
                {
                    int count = packet[4]*256 + packet[5];  
                    readHoldingRegister(count);             
                    break;
                }
                case 0x06: // 写保持寄存器
                {
                    //int count = packet[2]*256 + packet[3];  
                    writeSingleRegister(packet);             
                    break;
                }
                case 0x05: // 写保持寄存器
                {
                    //int count = packet[2]*256 + packet[3];  
                    digitalWrite(controller, HIGH); // 打开开关            
                    break;
                }
                default:
                {
                    Serial.println("其他指令，暂无法执行！"); // 其他指令暂不支持，不做响应
                    break;
                }
            }
        }
    }

    // 定时心跳发送数据
    if(HeartBeat >= HeartInterval*1000)
    {
        readHoldingRegister(1); 
        HeartBeat = 0;
    }
    HeartBeat++;
    delay(1);
}

// 注册传感器
void deviceRegister(byte id)
{
    byte data[7]; // 发送数据帧
    inputValue = analogRead(analogData) - 1;
    Serial.println("注册传感器...");
    // 设置传输数据帧
    data[0] = id;
    data[1] = 0x41;
    data[2] = 0x01;
    data[3] = byte(HeartInterval / 256);
    data[4] = byte(HeartInterval % 256);
    data[5] = byte();
    // 生成校验码
    byte *crc;
    crc = getCrc16(data, 5);
    data[5] = byte(crc[0]);// 存入低/高位
    data[6] = byte(crc[1]);
    // 向服务端发送注册请求包
    client.write(data, 7);
    Serial.println("已上传注册包！");
    delay(100);
}

// 上传数据
void readHoldingRegister(int count)
{
    int len = count * 2; // 数据字节数
    byte *data = (byte *)malloc(7 + len);    
    data[0] = deviceId; // packet[0]; // 设备地址
    data[1] = 0x03; // packet[1]; // 功能码
    data[2] = byte(2 + len & 0xff); // 数据字节数    
    data[3] = byte(HeartInterval / 256);
    data[4] = byte(HeartInterval % 256);
    inputValue = analogRead(analogData) - 1;
    //float h = dht.readHumidity();
    float h = (1023 - inputValue) * 100 / 650;
    Serial.print("湿度=");
    Serial.println(h);
    //Serial.println("水泵状态");
    for(int i = 5; i < 5 + len; i++)
    {
        if(i % 2)
            data[i] = byte(inputValue / 256);
        else
            data[i] = byte(inputValue % 256);
    }
    byte *src = getCrc16(data, 5 + len);
    data[5 + len] = src[0];// 存入低/高位
    data[6 + len] = src[1]; 
    Serial.println("设备ID：" + String(deviceId) + " [上送数据] .....");
    client.write(data, 7 + len);
    Serial.println("已上传传感器数据: " + String(inputValue));//String(data[3])
    delay(100);
}

// 写入修改数据
void writeSingleRegister(byte *data)
{
    byte dataH = data[4];
    byte dataL = data[5];
    if(dataH == 0x3b)
    {         
        if(dataL == 0x01)
        {
            digitalWrite(controller, HIGH);
            Serial.println("打开水泵！");
        }
        else
        {
            digitalWrite(controller, LOW);
            Serial.println("关闭水泵！");
        }
    }
    else if(dataH == 0x10)// 保留功能
    { }
    else// if(dataH == 0x20)
    {
        HeartInterval = data[4]*256 + data[5];
        Serial.println("修改心跳间隔为：" + String(HeartInterval));
    }
    delay(10);
}


void sendData()
{
    inputValue = analogRead(analogData);
    Serial.print("当前湿度 = ");
    Serial.print(inputValue - 1);
    Serial.println("");
}


//int len(byte *s)
//{
//    int i = 0;
//    while(s[i] >= 0x00 && s[i] <= 0xff)
//    {
//        i++;  
//    }
////    int n;
//    i = ((char)s).length;
//    Serial.println("i = " + i);
//    return i;
//}

/**
* 将int转换成byte数组，低位在前，高位在后 改变高低位顺序只需调换数组序号
*/
byte* intToBytes(int value) 
{
    byte *src;
    src = (byte *)malloc(2);
    src[0] = byte(value & 0xFF);
    src[1] = byte((value >> 8) & 0xFF);
    return src;
}

int bytes2Int(byte* b, int len)
{
    int r = b[len - 1] & 0xFF;
    for (int i = 1; i < len; i++) 
    {
        r += (b[len - i - 1] & 0xFF) << (i << 3);
    }
    return r;
}

byte* getCrc16(byte* arr_buff, int len) 
{
    //int leng = len(arr_buff);// 3 + arr_buff[2]; //
    //Serial.println(leng);
    // 预置 1 个 16 位的寄存器为十六进制FFFF, 称此寄存器为 CRC寄存器。
    int crc = 0xFFFF;
    int i, j;
    for (i = 0; i < len; i++) 
    {
        // 把第一个 8 位二进制数据 与 16 位的 CRC寄存器的低 8 位相异或, 把结果放于 CRC寄存器
        crc = ((crc & 0xFF00) | (crc & 0x00FF) ^ (arr_buff[i] & 0xFF));
        for (j = 0; j < 8; j++) 
        {
            // 把 CRC 寄存器的内容右移一位( 朝低位)用 0 填补最高位, 并检查右移后的移出位
            if ((crc & 0x0001) > 0) 
            {
                // 如果移出位为 1, CRC寄存器与多项式A001进行异或
                crc = crc >> 1;
                crc = crc ^ 0xA001;// 0xA001
            } 
            else 
                crc = crc >> 1; // 如果移出位为 0,再次右移一位
        }
    }
    return intToBytes(crc);
}
  
/**
 * 校验数据帧是否出错
 * @param arr_buff 数据帧
 * @return
*/
int check(byte* arr_buff, int len)
{
    byte* result = getCrc16(arr_buff, len);   
    int re = bytes2Int(result, 2);
    if (re == 0)
    {
        Serial.println("client数据校验正确");
        return 1;
    }
    else
    {
        Serial.println("client数据校验错误");
        return 0;
    }
}
