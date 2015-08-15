/*


*/
var context = {};

function computeContextFromForm(form) {
	$.each($(form).serializeArray(), function(i, field) {
		context[field.name] = field.value;
	});
}

function signIn(event) {
	console.log('Signing in...');
	computeContextFromForm('#sign-form');
	console.log(context);
	var result = false;
	$.ajax({
		method: "GET",
		url: "http://localhost:8080/v1/login",
		cache: false,
		headers : {
			'x-magic-app-id':  context.id,
			Authorization: 'Basic ' + btoa(context.username + ':' + context.password)
		},
		success: function() {
			$('#main').empty();
			result = true;
		},
		error: function(jqxhr, textStatus, errorThrown) {
			console.log(textStatus + ': ' + errorThrown);
			console.log(jqxhr);
			$('#error').empty().append(errorThrown + ': ').append(jqxhr.responseJSON.error.message);
		}
	});
	return result;
}

function createAccount(event) {
	console.log('Creating account...');
	computeContextFromForm('#create-form');
	console.log(context);
	var result = false;
	$.ajax({
		method: 'POST',
		url: 'http://localhost:8080/v1/account',
		cache: false,
		contentType: 'application/json; charset=UTF-8',
		data: JSON.stringify(context),
		processData: false,
		success: function() {
			$('#main').empty();
			result = true;
		},
		error: function(jqxhr, textStatus, errorThrown) {
			console.log(textStatus + ': ' + errorThrown);
			console.log(jqxhr);
			$('#error').empty().append(errorThrown + ': ').append(jqxhr.responseJSON.error.message);
		}
	});
	return result;
}

function init() {
	$('#sign-in').on('click', signIn);
	$('#create-account').on('click', createAccount);
}

$(document).ready(init);