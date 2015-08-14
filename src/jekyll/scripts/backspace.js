/*


*/

function signIn(event) {
	console.log('Signing in...');
	return true;
}

function createAcompte(event) {
	console.log('Creating acompte...');
	return true;
}

function init() {
	$('#sign-in').on('click', signIn);
	$('#create-acompte').on('click', createAcompte);
}

$(document).ready(init);