const express = require('express');
const router 	= express.Router();
const http 	= require('http');

// Reading content from URL
function fromExtLink(host, callback){
	http.get(host, function(res){
			let str = '';

			res.on('data', function (chunk) {
				str += chunk;
			});

			res.on('end', function () {
				callback(str);
			});

	}).on("error", function (){
		callback("GET request error");
	});
}

/* GET home page. */
router.get('/', function(req, res) {
	res.render('index', { layout: 'default', title: 'Fintan: Data manipulation platform' });
});

// router.get('/about', function(req, res) {
// 	res.render('about', { layout: 'front', title: 'About Teanga - Teanga • A Natural Language Processing Platform ●•·' });
// });

// router.get('/documents', function(req, res) {
// 	res.render('documents', { layout: 'front', title: 'Documentation - Teanga • A Natural Language Processing Platform ●•·' });
// });
//
// router.get('/publications', function(req, res) {
// 	res.render('publications', { layout: 'front', title: 'Publications - Teanga • A Natural Language Processing Platform ●•·' });
// });

router.post('/getdata', function(req, res) {
	const action = req.body.action;
	fromExtLink(action, function(returnValue){
		res.send(returnValue);
	});
});

router.post('/writejson', function(req) {
	const fs = require('fs');

	const action = req.body.action;

	fs.writeFile("/static/json/schema", "hello", function (err) {
		if (err) {
			return console.log(err);
		}

		console.log("The file was saved!");
	});
});

module.exports = router;
