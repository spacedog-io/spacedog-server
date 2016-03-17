/*
 *  © David Attias 2015
 */


var	$consoleDiv;
var $results;

function searchOk(data, textStatus, jqXHR) {
	inform('[' + data.total + '] result(s), [' + data.results.length
		 + '] displayed, search took [' + data.took + '] millisecond(s)');
	console.log(data);
	
	$results.empty();
	for (i=0; i < data.results.length; i++)
		$results.append('<pre class="prettyprint"><code>'
			 + JSON.stringify(data.results[i], null, 4)
			 + '</code></pre>')

	prettyPrint();
}

function searchObjects(event) {
	console.log('Searching objects ...');									

	$.ajax({
		method: "GET",
		url: backendUrl("/1/data"),
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

		if (sessionStorage.logInOk) {
			$consoleDiv = $('#console');
			$results = $('#console-results');

			$('#user-info').html('<p>Hello [' + sessionStorage.username
					 + ']</p><p>Your backend is [' + sessionStorage.backendId + ']</p>');

			$('#console-search-form').submit(searchObjects);
			
			$consoleDiv.css('visibility', 'visible');
			$consoleDiv.css('display', 'block');
			$consoleDiv.find('[name="q"]').focus();

		}
		else {
			window.location.assign('/log-in.html');			
		}

}

$(document).ready(init);