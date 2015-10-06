/*
*/

var	$consoleDiv;
var	$alert;
var $results;

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

function searchObjects(event) {
	console.log('Searching objects ...');									

	$.ajax({
		method: "GET",
		url: window.location.origin + "/v1/data",
		cache: false,
		headers : {
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

function init() {

		if (sessionStorage.signInOk) {
			$consoleDiv = $('#console');
			$alert = $('#search-alert');
			$results = $('#console-results');

			$('#user-info').html('<p>Hello [' + sessionStorage.username
					 + ']</p><p>Your spacedog key is</p><p>' + sessionStorage.spacedogKey + '</p>');

			$('#console-search-form').submit(searchObjects);
			
			$consoleDiv.css('visibility', 'visible');
			$consoleDiv.css('display', 'block');
			$consoleDiv.find('[name="q"]').focus();

		}
		else {
			window.location.assign('/sign-in.html');			
		}

}

$(document).ready(init);