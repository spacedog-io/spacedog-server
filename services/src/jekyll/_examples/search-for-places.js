/*
*/

var map;
var $searchForm;
var markers;
var poiInfo;
var poiPopUp;

function searchNok(jqxhr, textStatus, errorThrown) {
	var error = '=> Error: ';
	if (errorThrown) error +=  errorThrown;
	if (jqxhr.responseJSON) error += ': ' + jqxhr.responseJSON.error.message;
	console.log(error);
}

/*
function searchMappyOk(data, textStatus, jqXHR) {
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

function searchMappyPois(event) {
	console.log('Searching for Mappy pois ...');									

	$.ajax({
		method: "GET",
		url: "http://search.mappy.net/search/1.0/find",
		data: {
			bbox: map.getBounds().toUrlValue(),
			q: 'Restaurant',
			max_results: 100
		},
		success: searchMappyOk,
		error: searchNok
	});

	$searchForm.find('input[name="q"]').focus();
	return false;
}
*/

function searchInOk(data, textStatus, jqXHR) {
	if (data.results.length == 0)
		searchOutPois();
	else {
		displayPois(data);
		if (data.results.length == 1) {
			google.maps.event.trigger(markers[data.results[0].meta.id], 'click');
		}
	}
}

function searchOutOk(data, textStatus, jqXHR) {
	var bounds = displayPois(data);
	if (data.results.length > 0) {
		map.fitBounds(bounds);
		if (data.results.length == 1) {
			google.maps.event.trigger(markers[data.results[0].meta.id], 'click');
		}
	}
}


function displayPois(data) {

	console.log('pois: ', data);
	
	var dataMap = {};
 	var bounds = new google.maps.LatLngBounds();


	for (i=0; i<data.results.length; i++) {
		dataMap[data.results[i].meta.id] = data.results[i];
	}

	for (var poiId in markers) {
		if (dataMap[poiId]) {
			delete dataMap[poiId];
	 		bounds.extend(markers[poiId].getPosition());
		}
		else {
			markers[poiId].setMap(null);
			delete markers[poiId];
		}
	}

	for (var poiId in dataMap) {
		markers[poiId] = new google.maps.Marker({
			map: map,
			position: {lat: dataMap[poiId].where.lat, lng: dataMap[poiId].where.lon},
			title: poiId,
			label: dataMap[poiId].name,
			cursor: 'pointer',
			clickable: true
		});

 		bounds.extend(markers[poiId].getPosition());

		var marker = markers[poiId];
		markers[poiId].addListener('click', function() {
			console.log(this);
			poiPopUp.close();

			var poi = dataMap[this.title];
			$poiInfo.find("#title").text(poi.name);
			$poiInfo.find("#category").text(poi.rubrics[0].rubricLabel);
			$poiInfo.find("#address").text(poi.way + ' ' + poi.zipcode + ' ' + poi.town);
			var url = poi.url ? poi.url : '';
			$poiInfo.find("#url").attr('href', url);
			$poiInfo.find("#url").text(url);
			var illustration = poi.illustration ? poi.illustration : '';
			$poiInfo.find("#img").attr('src', illustration);

			poiPopUp.open(map, this);
		});
	}

	return bounds;
}

function searchInPois() {
	console.log('Searching in for pois ...');									

	var q = $searchForm.find('input[name="q"]').val();
	if (q=='') q = '*';
	
	$.ajax({
		method: "POST",
		url: backendUrl('/1/search/resto', 'examples'),
		processData: false,
		data: JSON.stringify({
			from: 0,
			size: 100,
			sort: ['illustration'],
			query: {
				bool: {
					must: {
						"query_string": {query: q}
					},
					filter: {
						"geo_bounding_box": {
							"where": {
								"top_left": {
									lat: map.getBounds().getNorthEast().lat(),
									lon: map.getBounds().getSouthWest().lng()
								},
								"bottom_right": {
									lat: map.getBounds().getSouthWest().lat(),
									lon: map.getBounds().getNorthEast().lng()
								}
							}
						}
					}
				}
			}
		}),
		success: searchInOk,
		error: searchNok
	});

	$searchForm.find('input[name="q"]').focus();
	return false;
}

function searchOutPois() {
	console.log('Searching out for pois ...');									

	var q = $searchForm.find('input[name="q"]').val();
	if (q=='') q = '*';
	
	$.ajax({
		method: "POST",
		url: backendUrl('/1/search/resto', 'examples'),
		processData: false,
		data: JSON.stringify({
			from: 0,
			size: 100, 
			query: {
				"query_string": {query: q}
			}
		}),
		success: searchOutOk,
		error: searchNok
	});

	$searchForm.find('input[name="q"]').focus();
	return false;
}

function initMap() {
	$searchForm = $('#map-search-form');
	
	$poiInfo = $('#poiInfo');
	poiPopUp = new google.maps.InfoWindow({
		content: $poiInfo[0],
		zIndex: 200
	});
	$poiInfo.css('display', 'block');
	
	markers = {};
	map = new google.maps.Map(document.getElementById('map'), {
		center: {lat: 48.85341, lng: 2.3488},
		zoom: 11,
		minZoom: 10,
		maxZoom: 16,
		streetViewControl: false,
		panControl: true,
		panControlOptions: {
			position: google.maps.ControlPosition.RIGHT_BOTTOM
		},
		zoomControl: true,
		zoomControlOptions: {
			position: google.maps.ControlPosition.RIGHT_BOTTOM,
			style: google.maps.ZoomControlStyle.LARGE
		},
		mapTypeControl: false
	});

	google.maps.event.addListenerOnce(map, 'tilesloaded', searchInPois);	
	$searchForm.submit(searchInPois)
	$searchForm.find('input[name="q"]').focus();
}