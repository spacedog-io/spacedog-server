/*
 *  © David Attias 2015
 */

var $navSignBt;
var $navConsoleLk;


function resetNavSignConsole() {
	if (sessionStorage.signInOk) {
		$navConsoleLk.attr('href', '/console.html');
		$navSignBt.html('Sign out');
		$navSignBt.one('click', function () {
			sessionStorage.removeItem('signInOk');
			$navConsoleLk.attr('href', '');
			window.location.assign('/sign-in.html');
			return false;
		});
	} else {
		$navSignBt.html('Sign in');
		$navSignBt.click(function () {
			window.location.assign('/sign-up.html');
		});
		$navConsoleLk.click(function () {
			window.location.assign('/sign-up.html');
		});
	}
}

function init() {

	$navSignBt = $('#nav-sign-bt');
	$navConsoleLk = $('#nav-console-lk');
	if ($navSignBt || $navConsoleLk) resetNavSignConsole();
}

$(document).ready(init);