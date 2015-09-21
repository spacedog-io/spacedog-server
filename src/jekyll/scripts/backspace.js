/*
*/

var	$signDiv;
var	$consoleDiv;
var	$alert;
var $results;
var $navSignBt;

function computeContextFromForm(form) {
	$.each($(form).serializeArray(), function(i, field) {
		sessionStorage[field.name] = field.value;
	});
	console.log(sessionStorage);
}

function showSearchDiv() {
	sessionStorage.signInOk = 'true';
	resetNavSignButton();

	$('#user-info').html('<p>Hello [' + sessionStorage.username
		+ ']</p><p>Your application id is [' + sessionStorage.id + ']</p>');
	$signDiv.css('visibility', 'hidden');
	$signDiv.css('display', 'none');
	$consoleDiv.css('visibility', 'visible');
	$consoleDiv.css('display', 'block');
	$consoleDiv.find('[name="q"]').focus();
	$alert = $('#search-alert');
}

function showSignDiv() {
	resetNavSignButton();
	
	$signDiv.css('visibility', 'visible');
	$signDiv.css('display', 'block');
	$consoleDiv.css('visibility', 'hidden');
	$consoleDiv.css('display', 'none');
	
	$('#sign-form').find('[name="username"]').focus();
	$consoleDiv.css('visibility', 'hidden');
	$alert = $('#sign-alert');
}

function searchOk(data, textStatus, jqXHR) {
	$alert.html('=> [' + data.total + '] result(s), [' + data.results.length + '] displayed, search took [' + data.took + '] millisecond(s)');
	console.log(data);
	
	$results.empty();
	for (i=0; i < data.results.length; i++)
		$results.append('<pre class="prettyprint"><code>'
			 + JSON.stringify(data.results[i], null, 4)
			 + '</code></pre><br>')

	prettyPrint();
}

function showError(jqxhr, textStatus, errorThrown) {
	console.log(textStatus + ': ' + errorThrown);
	console.log(jqxhr);

	if (errorThrown) $alert.html('=> ' +  errorThrown);
	else $alert.html('=> ' + textStatus);
	if (jqxhr.responseJSON) $alert.append(': ' + jqxhr.responseJSON.error.message);
}

function signIn(event) {
	console.log('Signing in...');
	computeContextFromForm('#sign-form');

	$.ajax({
		method: "GET",
		url: "http://localhost:8080/v1/login",
		cache: false,
		headers : {
			'x-magic-app-id':  sessionStorage.id,
			Authorization: 'Basic ' + btoa(sessionStorage.username + ':' + sessionStorage.password)
		},
		success: showSearchDiv,
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
		url: 'http://localhost:8080/v1/account',
		cache: false,
		contentType: 'application/json; charset=UTF-8',
		data: JSON.stringify(sessionStorage),
		processData: false,
		success: showSearchDiv,
		error: showError
	});

	$('#create-account').blur();
	return false;
}

function searchObjects(event) {
	console.log('Searching objects ...');									

	$.ajax({
		method: "GET",
		url: "http://localhost:8080/v1/data",
		cache: false,
		headers : {
			'x-magic-app-id':  sessionStorage.id,
			Authorization: 'Basic ' + btoa(sessionStorage.username + ':' + sessionStorage.password)
		},
		data: {
			q: $('input[name="q"]').val(),
			from: 0,
			size: 25
		},
		success: searchOk,
		error: showError
	});

	$consoleDiv.find('input[name="q"]').focus();
	return false;
}

function resetNavSignButton() {
	if (sessionStorage.signInOk) {
		$navSignBt.html('Sign out');
		$navSignBt.one('click', function () {
			sessionStorage.removeItem('signInOk');
			resetNavSignButton();
			if (window.location.pathname == '/console.html') showSignDiv();
			return false;
		});
	} else {
		$navSignBt.html('Sign in');
		$navSignBt.click(function () {
			window.location.assign('/console.html');
		});
	}
}

function init() {

	$navSignBt = $('#nav-sign-bt');
	resetNavSignButton();

	if (window.location.pathname == '/console.html') {

		$signDiv = $('#sign-div');
		$consoleDiv = $('#console');
		$alert = $('#sign-alert');
		$results = $('#console-results');

		$('#sign-form').submit(signIn);
		$('#create-form').submit(createAccount);
		$('#console-search-form').submit(searchObjects);

		if (sessionStorage.signInOk) showSearchDiv();
		else showSignDiv();
	}
}

$(document).ready(init);