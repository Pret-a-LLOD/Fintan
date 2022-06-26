const express = require('express');
const router  = express.Router();

const path 	= require('path');
const fs 	= require('fs');

const yaml1 = require('js-yaml');
function loadYaml(fileName) {
	var data = yaml1.safeLoad(fs.readFileSync(fileName,'utf8'));
	var indentedJson = JSON.stringify(data, null, 4);
	return data;
}

// return all JSON files in Schema directory
function fromDir(startPath,filter){
	if (!fs.existsSync(startPath)){
		console.log("no dir ",startPath);
		return;
	}
	var list = [];
	var files = fs.readdirSync(startPath);
	for(var i = 0; i < files.length; i++){
		var filename = path.join(startPath,files[i]);
		var stat = fs.lstatSync(filename);
		if (stat.isDirectory()){
			fromDir(filename,filter); //recurse
		}
		else if (filename.indexOf(filter)>=0) {
			filename = filename.substring(filename.indexOf("/") + 1);
			list.push(filename);
		}
	}
	return list;
}


router.get('/readJSON/:type/:filename', function (req, res) {
	let filename = req.params.type + '/' + req.params.filename;
	res.json(loadYaml('public/'+filename));
});

router.get('/create', function(req, res) {
	res.render('pipelines/create',
		{
			title: 'Fintan: Create a pipeline',
			jsonsArray_transformations: fromDir('./public/transformations', '.yaml'),
			jsonsArray_resources: fromDir('./public/resources', '.yaml'),
			jsonsArray_data: fromDir('./public/data', '.yaml'),
		});
});

router.get('/list', function(req, res) {
	res.render('pipelines/list',
		{
			title: 'Fintan: Available pipelines'
		});
});

router.get('/status', function(req, res) {
	res.render('tasks/status',
		{
			title: 'Fintan: Task execution status'
		});
});

module.exports = router;