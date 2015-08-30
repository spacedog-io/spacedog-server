/*
*/

var map;


function searchOk(data, textStatus, jqXHR) {
	console.log(data);

	for (i=0; i<data.pois.length; i++) {
		var poi = data.pois[i];
		new google.maps.Marker({
			map: map,
			position: {lat: poi.lat, lng: poi.lng},
			title: poi.name
		});
	}
}

function searchNok(jqxhr, textStatus, errorThrown) {
	console.log(textStatus + ': ' + errorThrown);
	console.log(jqxhr);

	if (errorThrown) $alert.html('=> ' +  errorThrown);
	else $alert.html('=> ' + textStatus);
	if (jqxhr.responseJSON) $alert.append(': ' + jqxhr.responseJSON.error.message);
}


function searchPois(event) {
	console.log('Searching pois ...');									

	$.ajax({
		method: "GET",
		url: "http://search.mappy.net/search/1.0/find",
		data: {
//			extend_bbox: 1,
			bbox: map.getBounds().toUrlValue(),
			q: 'Restaurant',
			max_results: 100
		},
		success: searchOk,
		error: searchNok
	});

	return false;
}

function initMap() {
  map = new google.maps.Map(document.getElementById('map'), {
    center: {lat: 48.85341, lng: 2.3488},
    zoom: 11
  });
  map.addListener('tilesloaded', searchPois);
}
