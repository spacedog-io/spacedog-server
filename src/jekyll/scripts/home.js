/*
 *  © David Attias 2015
 */

var $subscribeForm;
var $subscribe;
var $email;
var $response;

function showError(jqxhr) {
	console.log('jqxhr', jqxhr);
	$response.html('<br><p>=> ' +  jqxhr.statusText + ': ' + jqxhr.status + '</p>');
}

function showSuccess(response) {
	console.log('response', response);

	if (response.msg)
		$response.html('<br><p>=> ' + response.msg + '</p>');
	else
		$response.html('<br><p>=> Error: no message.</p>');
}

function subscribe(event) {
	console.log('Subscribing for new user...');

	$.ajax({
		url: '//spacedog.us11.list-manage.com/subscribe/post-json',
		cache: false,
		data: $subscribeForm.serialize(),
		jsonp: 'c',
		dataType: 'jsonp',
		success: showSuccess,
		error: showError
	});

	$subscribe.blur();
	return false;
}

function init() {
	$email = $('#email');
	$subscribe = $('#subscribe');
	$subscribeForm = $('#subscribe-form');
	$response = $('#subscribe-response');
	$subscribeForm.submit(subscribe);
}

$(document).ready(init);