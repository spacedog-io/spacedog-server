/*
 *  © David Attias 2015
 */

var $signDiv;
var $consoleDiv;
var $results;
var $navSignBt;

function computeContextFromForm(form) {
	$.each($(form).serializeArray(), function(i, field) {
		sessionStorage[field.name] = field.value;
	});
	console.log(sessionStorage);
}

function showConsole(_, _, jqxhr) {

	// called after log in or sign up
	if (jqxhr) {
		sessionStorage.logInOk = 'true';
		//sessionStorage.spacedogKey = jqxhr.getResponseHeader('x-spacedog-backend-key');
	}
	
	// or called when already logged in
	window.location.assign('/console.html');
}

function logIn(event) {
	console.log('Logging in...');
	computeContextFromForm('#sign-form');

	$.ajax({
		method: "GET",
		url: backendUrl("/1/admin/login"),
		cache: false,
		headers : {
			Authorization: 'Basic ' + btoa(sessionStorage.username + ':' + sessionStorage.password)
		},
		success: showConsole,
		error: showError
	});

	$('#log-in').blur();

	return false;
}

function createAccount(event) {
	console.log('Creating account...');
	computeContextFromForm('#create-form');

	if (sessionStorage.password != sessionStorage.passwordConfirmation) {
		alert('Error: pasword confirmation is incorect');
	}
	else {
		$.ajax({
			method: 'POST',
			url: window.location.origin + '/1/backend/' + sessionStorage.backendId,
			cache: false,
			contentType: 'application/json; charset=UTF-8',
			data: JSON.stringify(sessionStorage),
			processData: false,
			success: showConsole,
			error: showError
		});

		$('#create-account').blur();
	}
	
	return false;
}

function init() {

	if (sessionStorage.logInOk) showConsole();
	else {
		$signDiv = $('#sign-div');

		if (window.location.pathname == '/log-in.html') {
			$signDiv.find('#sign-form').submit(logIn);
			$signDiv.find('[name="backendId"]').focus();
		} else if (window.location.pathname == '/sign-up.html') {
			$signDiv.find('#create-form').submit(createAccount);			
			$signDiv.find('[name="backendId"]').focus();
		}
	}
}

$(document).ready(init);