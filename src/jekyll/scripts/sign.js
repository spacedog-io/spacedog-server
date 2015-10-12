/*
*/

var $signDiv;
var $consoleDiv;
var $alert;
var $results;
var $navSignBt;

function computeContextFromForm(form) {
	$.each($(form).serializeArray(), function(i, field) {
		sessionStorage[field.name] = field.value;
	});
	console.log(sessionStorage);
}

function showConsole(_, _, jqxhr) {
	sessionStorage.signInOk = 'true';
	sessionStorage.spacedogKey = jqxhr.getResponseHeader('x-spacedog-backend-key');
	window.location.assign('/console.html');
}

function showError(jqxhr, textStatus, errorThrown) {
	console.log(textStatus + ': ' + errorThrown);
	console.log(jqxhr);

	if (errorThrown) $alert.html('=> ' +  errorThrown);
	else $alert.html('=> ' + textStatus);

	var message = _.get(jqxhr, 'responseJSON.error.message');
	if (message) $alert.append(': ' + message);
	var badParam = message = _.get(jqxhr, 'responseJSON.invalidParameters');
	if (badParam) $alert.append(': ' + _(badParam).values().map('message'));
}

function signIn(event) {
	console.log('Signing in...');
	computeContextFromForm('#sign-form');

	$.ajax({
		method: "GET",
		url: window.location.origin + "/v1/admin/login",
		cache: false,
		headers : {
			Authorization: 'Basic ' + btoa(sessionStorage.username + ':' + sessionStorage.password)
		},
		success: showConsole,
		error: showError
	});

	$('#sign-in').blur();
	return false;
}

function createAccount(event) {
	console.log('Creating account...');
	computeContextFromForm('#create-form');

	$.ajax({
		method: 'POST',
		url: window.location.origin + '/v1/admin/account',
		cache: false,
		contentType: 'application/json; charset=UTF-8',
		data: JSON.stringify(sessionStorage),
		processData: false,
		success: showConsole,
		error: showError
	});

	$('#create-account').blur();
	return false;
}

function init() {

	if (sessionStorage.signInOk) showConsole();
	else {
		$signDiv = $('#sign-div');
		$alert = $('#sign-alert');

		if (window.location.pathname == '/sign-in.html') {
			$('#sign-form').submit(signIn);
		} else {
			$('#create-form').submit(createAccount);			
		}
	}
}

$(document).ready(init);