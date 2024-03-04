// Requires jQuery

function refresh() {
	$.ajax({
		type: "post",
		url: '/refrash',
		dataType: 'json',
		contentType: 'application/json;charset=UTF-8',//编码格式
		success: function (response) {
			$("#realtime_data").val(response.realtime_data);
			$("#note_data").val(response.note_data);
			console.log(response);
		},
		error: function (xhr, type) {
			console.log(xhr);
		}
	});
}
setTimeout(refresh, 3000)

$(document).on('click', '.js-menu_toggle.closed', function (e) {
	e.preventDefault();
	$('.list_load, .list_item').stop();
	$(this).removeClass('closed').addClass('opened');

	$('.side_menu').css({ 'left': '0px' });

	var count = $('.list_item').length;
	$('.list_load').slideDown((count * .6) * 100);
	$('.list_item').each(function (i) {
		var thisLI = $(this);
		timeOut = 100 * i;
		setTimeout(function () {
			thisLI.css({
				'opacity': '1',
				'margin-left': '0'
			});
		}, 100 * i);
	});
});

$(document).on('click', '#item_1', function (e) {
	$('#item_1').addClass('selected');
	$('#item_2').removeClass('selected');
	$('#item_3').removeClass('selected');

	$('#yckz').hide();
	$('#sjgl').hide();
	$('#sjjk').show(500);
	
});

$(document).on('click', '#item_2', function (e) {
	$('#item_2').addClass('selected');
	$('#item_1').removeClass('selected');
	$('#item_3').removeClass('selected');

	$('#sjjk').hide();
	$('#sjgl').hide();
	$('#yckz').show(500);
	
});

$(document).on('click', '#item_3', function (e) {
	$('#item_3').addClass('selected');
	$('#item_2').removeClass('selected');
	$('#item_1').removeClass('selected');

	$('#sjjk').hide();
	$('#yckz').hide();
	$('#sjgl').show(500);
	
});

$(document).on('click', '.js-menu_toggle.opened', function (e) {
	e.preventDefault();
	$('.list_load, .list_item').stop();
	$(this).removeClass('opened').addClass('closed');

	$('.side_menu').css({ 'left': '-250px' });

	var count = $('.list_item').length;
	$('.list_item').css({
		'opacity': '0',
		'margin-left': '-20px'
	});
	$('.list_load').slideUp(300);
});