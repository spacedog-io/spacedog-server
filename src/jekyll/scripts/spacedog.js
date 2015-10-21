/*
 *  © David Attias 2015
 */

var $navLogInBt;
var $navConsoleLk;
var $alert;

function alert(message) {
	$alert.html('<br><p>=> ' + message + '</p>');
	$alert.find('p').css('color', 'red');
}

function inform(message) {
	$alert.html('<br><p>=> ' + message + '</p>');
	$alert.find('p').css('color', 'beige');
}

function showError(jqxhr, textStatus, errorThrown) {
	console.log(textStatus + ': ' + errorThrown);
	console.log(jqxhr);

	if ($alert) {
		var m1;
		if (errorThrown) m1 = errorThrown;
		else m1 = textStatus;

		var m2 = _.get(jqxhr, 'responseJSON.error.message');
		if (m2) m1 += ': ' + m2;
		var badParam = _.get(jqxhr, 'responseJSON.invalidParameters');
		if (badParam) m1 += ': ' + _(badParam).values().map('message');

		alert(m1);
	}
}

function resetNavSignConsole() {
	if (sessionStorage.logInOk) {
		$navConsoleLk.attr('href', '/console.html');
		$navLogInBt.html('Log out');
		$navLogInBt.one('click', function () {
			sessionStorage.removeItem('logInOk');
			$navConsoleLk.attr('href', '');
			window.location.assign('/log-in.html');
			return false;
		});
	} else {
		$navLogInBt.html('Log in');
		$navLogInBt.click(function () {
			window.location.assign('/log-in.html');
		});
		$navConsoleLk.click(function () {
			window.location.assign('/log-in.html');
		});
	}
}

function init() {

	$alert = $('#alert-div');
	$navLogInBt = $('#nav-sign-bt');
	$navConsoleLk = $('#nav-console-lk');
	if ($navLogInBt || $navConsoleLk) resetNavSignConsole();
}

$(document).ready(init);