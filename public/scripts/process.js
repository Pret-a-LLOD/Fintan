window.onload = function() {

	$(document).ready(function() {


		// Initialize tooltips to show description of service in the services area
		$(function () {
			$('[data-toggle="tooltip"]').tooltip();
		});

		// Closing the options panel when the user clicks the little x
		$('.close').on('click', function() {
			$('#options').hide("slide", { direction: "right" }, 400);
			return false;
		});

		// listing the available colours in bootstrap library to colour the services bubbles
		const colours = ["success", "info", "warning", "danger"];

		// function to highlight JSON syntax for better display
		function syntaxHighlight(json) {
			json = json.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
			return json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function (match) {
				let cls = 'number';
				if (/^"/.test(match)) {
					if (/:$/.test(match)) {
						cls = 'key';
					} else {
						cls = 'string';
					}
				} else if (/true|false/.test(match)) {
					cls = 'boolean';
				} else if (/null/.test(match)) {
					cls = 'null';
				}
				return '<span class="' + cls + '">' + match + '</span>';
			});
		}

		// load json file to a variable
		function load_json_file(file){
			// file = file.replace(/transformations|services\//, '');
			var jsonFile = [];

			$.ajax({
				url: 'readJSON/'+file,
				async: false,
				dataType: 'json',
				success: function (json) {
					jsonFile = json;
					jsonFile.type = /^resources\//.test(file) ? 'resource' : 'transformation';
				}
			});

			return jsonFile;
		}

		// Apparently, it's not used anywhere
		// function objectSize(obj) {
		// 	let size = 0, key;
		// 	for (key in obj) {
		// 		if (obj.hasOwnProperty(key)) size++;
		// 	}
		// 	return size;
		// }

		// Function to prepare input and output data from the JSON LD File
		// it converts the input and output of a service to connectors on the
		// service bubbles
		// function foreach_data_js(jsonFile, entry){
		//
		// 	let allPaths = Object.values(jsonFile.paths);
		//
		// 	let allParams = allPaths[0].get.parameters;
		//
		// 	let count = allParams.length;
		//
		//
		// 	let entries_labels = ' data-nb-' + entry + 's-labels="';
		// 	let entries = ' data-nb-' + entry + 's-names="';
		//
		// 	let entry_name;
		// 	for (let i = 0; i < count; ++i) {
		// 		entry_name = "input_text";
		// 		entries += allParams[i].name + ", ";
		// 		entry_name = entry_name.replace(/[_-]/g, " ");
		// 		entries_labels = entries_labels + entry_name + ", ";
		// 	}
		//
		// 	entries = entries.replace(/, \s*$/, "")+'"';
		// 	entries_labels = entries_labels.replace(/, \s*$/, "")+'"';
		//
		// 	return entries+entries_labels+' data-nb-'+entry+'s="'+count+'"' ;
		// }

		function get_io_nodes(conf, entry) {
			let entries = conf[entry] === undefined || conf[entry] === null ? [] : conf[entry];
			let res = [];

			res.push('data-nb-' + entry + '="' + entries.length + '"');
			res.push('data-nb-' + entry + '-names' + '="' + entries.map(e => e.name).join(', ') + '"');
			res.push('data-nb-' + entry + '-labels' + '="'
				+ entries.map(e => e.label !== undefined ? e.label : 'input text').join(', ') + '"');

			if (conf.resources !== null && entry == 'resources') {
				res.push('data-nb-graphs="' + entries.map(e => e.source + "|" + e.graph).join(', ') + '"');
				//res.push('data-nb-graphs="' + entries.map(e => e.graph).join(', ') + '"');
			}

			return res;
		}

		// Function to prepare service options from the JSON LD File
		function get_options(jsonFile){

			let count;
			let options_labels;
			let options;

			if (jsonFile.options) {
				count = jsonFile.options.length;
				options_labels = ' data-nb-options-labels="';
				options = ' data-nb-options-names="';

				for (let i = 0; i < count; ++i) {
					const find_name = jsonFile.options[i];
					options_labels += find_name + ', ';
					options += jsonFile["@context"][find_name].name + ', ';
				}
				options = options.replace(/, \s*$/, "") + '"';
				options_labels = options_labels.replace(/, \s*$/, "") + '"';
				return ' data-nb-options="' + count + '" ' + options + options_labels;
			} else {
				return '';
			}
		}

		function get_bubbles(conf_files) {
			let bubbles = [];

			for (let key in conf_files) {

				let conf = load_json_file(conf_files[key]);
				let attrib = [];

				attrib.push('class="draggable_operator btn btn-' + colours[key % colours.length] + ' btn-circle btn-xl"');
				// attrib.push('data-nb-action=""');
				if (conf.info.path !== undefined) {
					attrib.push('data-nb-path="' + conf.info.path + '"');
				}

				if (conf.info.iter !== undefined) {
					attrib.push('data-nb-iter="' + conf.info.iter + '"');
				}

				attrib.push(get_io_nodes(conf, 'inputs'));
				attrib.push(get_io_nodes(conf, 'outputs'));
				attrib.push(get_io_nodes(conf, 'resources'));

				// TODO: figure this out
				// (I don't know how this works)
				// attrib.push(get_options(conf));

				attrib.push('data-nb-type="' + (conf.resources !== null ? 'model' : 'update') + '"');

				attrib.push('data-toggle="tooltip"');
				attrib.push('data-placement="top"');
				attrib.push('data-original-title="' + conf.info.description + '"');
				bubbles.push('<div ' + attrib.flat().join(' ') + '>' + conf.info.title + '</div>');
			}

			return bubbles.join('\n');
		}

		// injecting the transformations
		$("#transformations_area").html(get_bubbles(all_jsons));

		// injecting the resources
		$("#resources_area").html(get_bubbles(all_jsons_res));

		// the flowchart part
		const $flowchart = $('#chart_area');
		const $container = $flowchart.parent();

		// preparing input and output bubbles in the chart area
		const data = {
			operators: {
				input: {
					top: 20,
					left: 50,
					properties: {
						title: 'Input',
						options: {},
						inputs: {},
						outputs: {
							output_1: {
								label: 'Uploaded Data',
							}
						}
					}
				},
				output: {
					top: 220,
					left: 600,
					properties: {
						title: 'Result',
						options: {},
						inputs: {
							input_1: {
								label: 'Data Result',
							}
						},
						outputs: {},
						resources: {}
					}
				}
			}
		};

		// Preparing the flowchart plugin
		$flowchart.flowchart({
			data: data,
			distanceFromArrow: 0,
			defaultLinkColor: "#5CB85C",
			defaultSelectedLinkColor: "#D95360",
			verticalConnection: false,

			// listing behaviour on events
			onLinkSelect: function(linkId){
				$("g[data-link_id="+linkId+"] path").addClass("dashed-line");
				return true;
			},

			onLinkUnselect: function() {
				$(".dashed-line").removeClass("dashed-line");
				return true;
			},

			onOperatorSelect: function(operatorId) {

				// on selecting the service, show options panel if it contains
				// let operator_options = $flowchart.flowchart('getOperatorOptions', operatorId);
				// let options_count = Object.keys(operator_options).length;
				// let options_contents;
				//
				// if (options_count > 0) {
				// 	options_contents = '';
				// 	for (let i = 0; i < options_count; ++i) {
				// 		let option_label = operator_options["option_" + i].label;
				// 		let option_name = operator_options["option_" + i].name;
				// 		options_contents += option_label.replace(/[_-]/g, " ") +
				// 			'<select class="form-control" name="' + option_name + '" id="' + option_name + '">' + languages + '</select><br>';
				// 	}
				// 	$("#optionsContent").html(options_contents);
				// 	$("#options").show("slide", { direction: "right" }, 400);
				// } else {
				// 	$("#options").hide("slide", { direction: "right" }, 400);
				// }
				return true;
			},

			onOperatorUnselect: function() {
				return true;
			}
		});

		// delete service or link button
		$('.delete_selected_button').click(function(event) {
			event.preventDefault();
			$flowchart.flowchart('deleteSelected');
		});

		// declaring process list here to be accessible later
		let process_list = {};
		let fintan_config = {};

		let theWizard = $("#smartwizard");
		theWizard.on("showStep", function(e, anchorObject, stepNumber) {
			// FIXME temporary: there will be a Step 1, so this should work normally. Otherwise, set up Wizard properly
			// FIXME also, there are two event handlers for this
			if (stepNumber === 1 || stepNumber === 2) {

				// event to fire up on button click in step 2 to collect the data from the graph
				// and convert it to a JSON file
				let data = $flowchart.flowchart('getData');
				let links_count = Object.keys(data.links).length;
				let operators_count = Object.keys(data.operators).length;

				// filling the options from the select drop downs into the JSON
				// $.each($("select"), function () {
				// 	let option_name = $(this).attr("id");
				// 	let option_value = $(this).val();
				// 	for (let key in data.operators) {
				// 		if (key === "undefined") continue;
				// 		if (data.operators[key].properties !== "undefined") {
				// 			if (Object.keys(data.operators[key].properties.options.length > 0)) {
				// 				for (let smallkey in data.operators[key].properties.options) {
				// 					if (data.operators[key].properties.options[smallkey].name === option_name) {
				// 						data.operators[key].properties.options[smallkey].value = option_value;
				// 					}
				// 				}
				// 			}
				// 		}
				// 	}
				// });

				// check if there is options that aren't filled
				let error = '';
				// for (let key in data.operators) {
				// 	if (key === "undefined") continue;
				// 	if (data.operators[key].properties !== "undefined") {
				// 		if (Object.keys(data.operators[key].properties.options).length > 0) {
				// 			for (let smallkey in data.operators[key].properties.options) {
				// 				if (data.operators[key].properties.options[smallkey].value === "") {
				// 					error = "Please select options for the " + data.operators[key].properties.title;
				// 				}
				// 			}
				// 		}
				// 	}
				// }

				let checking_data;

				// checking if there is any error before continuing
				if (links_count < 1 || operators_count < 2) {
					alert("Please add services and / or connect them.");

				} else if (error !== '') {
					alert(error);

				} else {
					let text_holder_value = $("#text_holder").val();
					// checking_data = "<p>You textual data is </p><pre>" + text_holder_value + "</pre><br>";
					checking_data = '';
					checking_data += '<p>Transformations </p>';

					// recursive function to find the right order of the services
					// it starts from input and follow the links till the end
					let the_order = ["input"];
					let services_order = function (items, all_length, attribute, value, the_order) {
						for (let i = 0; i < all_length; i++) {
							if (typeof items[i] !== "undefined") {
								if (items[i][attribute] === value) {
									let find_more = items[i].toOperator;
									if (find_more === "undefined") continue;
									the_order.push(find_more);
									services_order(items, all_length, attribute, find_more, the_order);

									return the_order;
								}
							}
						}
					};

					let model_connections = function (items, connector_rx) {
						connections = {};

						for (let i = 0; i < Object.keys(items).length; i++) {
							if ((items[i].fromConnector.match(connector_rx))) {
								if (connections[items[i].toOperator] === undefined) {
									connections[items[i].toOperator] = [];
								}
								connections[items[i].toOperator].push(items[i].fromOperator);
							}
						}

						return connections;
					};

					let order_list = services_order(data.links, links_count, "fromOperator", "input", the_order);
					let models = model_connections(data.links, /resource.*/);

					// showing steps and options on Step 3
					let x = 1;
					let all_options = '';

					// process_list.text = text_holder_value;
					process_list.services = {};

					fintan_config.input = "System.In";
					fintan_config.output = "System.Out";
					fintan_config.pipeline = [];

					let arrayLength = order_list.length;
					for (let i = 0; i < arrayLength; i++) {
						let key = order_list[i];

						let service_title = data.operators[key].properties.title;

						if (key !== "input" && key !== "output") {
							fintan_config.pipeline.push({
								class: 'CoNLLRDFUpdater',
								models: models[key] === undefined ? [] :
									models[key].map(item => ({
										source: data.operators[item].properties.options.models[0][0],
										graph: data.operators[item].properties.options.models[0][1]})),
								updates: data.operators[key].properties.options.updates});
						}

						let loadedData = '';
						checking_data += '<pre class="process-steps">Step ' + x + ': ' + service_title + loadedData + '</pre>';

						all_options = '';
						x++;
					}

					// Outputting the final config
					checking_data += '<p>Fintan JSON configuration:</p>';
					checking_data += '<pre>' + JSON.stringify(fintan_config, null, 2) + '</pre>';

					// filling the step 3 with results and continue if everything is ok
					$("#check_workflow").html(checking_data);
				}
			}
		});
	
		// function to get the data from the operators
		function getOperatorData($element) {
			let nbInputs = parseInt($element.data('nb-inputs'));
			let nbOutputs = parseInt($element.data('nb-outputs'));
			let nbResources = parseInt($element.data('nb-resources'));
			let nbOptions = parseInt($element.data('nb-options'));

			let operatorType = $element.data('nb-type');

			let nbPath = $element.data('nb-path');
			let nbIter = $element.data('nb-iter');
			let nbGraphs = $element.data('nb-graphs');
			nbGraphs = nbGraphs ? nbGraphs.split(', ') : undefined;

			let data = {
				properties: {
					title: $element.text(),
					action: $element.data('nb-action'),
					options: {
						'type': operatorType
					},
					inputs: {},
					outputs: {},
					resources: {}
				} 
			};

			if (operatorType === 'model') {
				data.properties.options.models = nbGraphs.map(e => e.split('|'));
			}
			else {
				data.properties.options.updates = [{'path': nbPath, 'iter': nbIter}];
			}

			let nbInputsLabels = $element.data('nb-inputs-labels').split(", ");
			let nbOutputsLabels = $element.data('nb-outputs-labels').split(", ");
			let nbResourcesLabels = $element.data('nb-resources-labels').split(", ");

			let nbInputsNames = $element.data('nb-inputs-names').split(", ");
			let nbOutputsNames = $element.data('nb-outputs-names').split(", ");
			let nbResourcesNames = $element.data('nb-resources-names').split(", ");

			for (let i = 0; i < nbInputs; i++) {
				data.properties.inputs['input_' + i] = {
					label: nbInputsLabels[i],
					name: nbInputsNames[i]
				};
			}

			for (let i = 0; i < nbOutputs; i++) {
				data.properties.outputs['output_' + i] = {
					label: nbOutputsLabels[i],
					name: nbOutputsNames[i]
				};
			}

			for (let i = 0; i < nbResources; i++) {
				data.properties.resources['resource_' + i] = {
					label: nbResourcesLabels[i],
					name: nbResourcesNames[i],
				};
			}

			if (nbResources > 0) {
				data.properties.class = 'flowchart-resource-operator';
			}

			if($element.data('nb-options-names')) {
				let nbOptionsLabels = $element.data('nb-options-labels').split(", ");
				let nbOptionsNames = $element.data('nb-options-names').split(", ");
				for (let i = 0; i < nbOptions; i++) {
					data.properties.options['option_' + i] = {
						label: nbOptionsLabels[i],
						name: nbOptionsNames[i],
						value: ""
					};
				}
			}

			return data;
		}
	
		// making the operators draggable
		let operatorId = 0;
		let $draggableOperators = $('.draggable_operator');
		$draggableOperators.draggable({
			cursor: "move",
			opacity: 0.7,
			appendTo: 'body',
			zIndex: 1000,
			helper: function() {
				let $this = $(this);
				let data = getOperatorData($this);
				return $flowchart.flowchart('getOperatorElement', data);
			},
			stop: function(e, ui) {
				let $this = $(this);
				let elOffset = ui.offset;
				let containerOffset = $container.offset();
				if (elOffset.left > containerOffset.left &&
					elOffset.top > containerOffset.top && 
					elOffset.left < containerOffset.left + $container.width() &&
					elOffset.top < containerOffset.top + $container.height()) {

					let flowchartOffset = $flowchart.offset();

					let relativeLeft = elOffset.left - flowchartOffset.left;
					let relativeTop = elOffset.top - flowchartOffset.top;

					let positionRatio = $flowchart.flowchart('getPositionRatio');
					relativeLeft /= positionRatio;
					relativeTop /= positionRatio;
					
					let data = getOperatorData($this);
					data.left = relativeLeft;
					data.top = relativeTop;
					
					$flowchart.flowchart('addOperator', data);
				}
			}
		});

		theWizard.on("showStep", function(e, anchorObject, stepNumber) {
			if (stepNumber === 3) {

				let tasks_count = Object.keys(process_list.services).length;
				let semaphore  = 1; // semaphoring the process to handle the pipelines
				let textual_data = process_list.text;

				setTimeout(function() {
					for (let i = 0; i < tasks_count; i++) {
						if (semaphore === 1){
							semaphore--;

							let this_service 	= process_list.services[i];
							let service_name 	= this_service.name;
							let service_url 	= this_service.action;
							let service_input 	= this_service.input;
							let service_output 	= "output"; //this_service.output

							let operator;
							if (service_url.indexOf('?') > -1){
								operator = "&";
							} else {
								operator = "?";
							}

							$("#results_message").text("Contacting " + service_name + ", please wait ...");
							let complete_url = 'http://' + service_url + '/' + operator + service_input + '=' + textual_data.replace(/ /g, '+');

							if(this_service.options){
								let service_options = '';
								let options_list = this_service.options;
								for (let key in options_list) {
								    let value = options_list[key];
								    service_options += '&' + key + '=' + value;
								}
								complete_url += service_options;
							}

							//console.log(complete_url);

							$.ajax({
								type: "POST",
								url: "/getdata",
								async:false,
								data: {
									"action" : encodeURI(complete_url)
								},
								success: function(resultData){

									if (resultData != "GET request error") {

										let jsondata = JSON.parse(resultData);
										textual_data = findinJson(jsondata, service_output);

										resultData = JSON.stringify(JSON.parse(resultData),null,2);
										resultData = syntaxHighlight(resultData);

										$("#results").prepend('<div class="panel panel-default" id="panel'+i+'"><div class="panel-heading results"><h4 class="panel-title"><a data-toggle="collapse" data-target="#collapse'+i+'" href="#collapse'+i+'">Result from '+service_name+'</a></h4></div><div id="collapse'+i+'" class="panel-collapse collapse in show"><div class="panel-body"><p class="truncate">The results: "'+textual_data+'" <br>The service URL: <a href="'+complete_url+'">'+complete_url+'</a></p><br><pre>'+resultData+'</pre></div></div></div>');

										semaphore++;

										$("#results_message").text("Finished, all of your results are displayed below.");
									} else {
										$("#results_message").text("Error contacting the service");
									}
								}
							});
						}
					}
				}, 100);
			}
		});
	}); // closing the jquery ready wrapper

	let languages = '<option value="">Please Select</option><option value="bg">Bulgarian (bg)</option><option value="hr">Croatian (hr)</option><option value="cs">Czech (cs)</option><option value="da">Danish (da)</option><option value="nl">Dutch (nl)</option><option value="en">English (en)</option><option value="et">Estonian (et)</option><option value="fi">Finnish (fi)</option><option value="fr">French (fr)</option><option value="de">German (de)</option><option value="el">Greek (el)</option><option value="hu">Hungarian (hu)</option><option value="ga">Irish (ga)</option><option value="it">Italian (it)</option><option value="lv">Latvian (lv)</option><option value="lt">Lithuanian (lt)</option><option value="mt">Maltese (mt)</option><option value="pl">Polish (pl)</option><option value="pt">Portuguese (pt)</option><option value="ro">Romanian (ro)</option><option value="sk">Slovak (sk)</option><option value="sl">Slovene (sl)</option><option value="es">Spanish (es)</option><option value="sv">Swedish (sv)</option>';


	// function to find a result under many levels inside a json
	function findinJson(json, output){
		let result;
		if (output === "all_file"){
			result = JSON.stringify(json);
		} else if(output.indexOf('.') > -1) {
			let array = output.split(".");
			for (let i = 0; i < array.length; i++) {
				let item = array[i];
				json = json[item];
			}
			result = json;

		} else {
			result = json[output];
		}
		return result;
	}

}; // closing the javascript ready wrapper
