const express = require('express');
const path = require('path');
const logger = require('morgan');
const cookieParser = require('cookie-parser');
const bodyParser = require('body-parser');
const exphbs = require('express-handlebars');

const hbs = exphbs.create({
	helpers:{
			/* small helpful functions can go here */
			if_eq: function(a, b, opts) {
					if(a === b) // Or === depending on your needs
							return opts.fn(this);
					else
							return opts.inverse(this);
			},
			humanFileSize: function (bytes, si) {
					let thresh = si ? 1000 : 1024;
					if(Math.abs(bytes) < thresh) {
							return bytes + ' B';
					}
					let units = si ? ['kB','MB','GB','TB','PB','EB','ZB','YB']
							: ['KiB','MiB','GiB','TiB','PiB','EiB','ZiB','YiB'];
					let u = -1;
					do {
							bytes /= thresh;
							++u;
					} while(Math.abs(bytes) >= thresh && u < units.length - 1);
					return bytes.toFixed(1)+' '+units[u];
			},
			jsonifyThis: function(obj) {
				return JSON.stringify(obj, null, 3);
			}
	},
	extname: 'hbs',
	defaultLayout: 'default',
	layoutsDir: __dirname + '/views/layouts/'
});

const index = require('./routes/index');
const pipelines = require('./routes/pipelines');

const app = express();

// view engine setup
app.engine('hbs', hbs.engine);
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'hbs');
app.set('port', 5007);

app.use(logger('dev'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));
app.use(cookieParser());

app.use('/', index);
app.use('/pipelines/', pipelines);

app.use(express.static('public'));

// catch 404 and forward to error handler
app.use(function(req, res, next) {
	let err = new Error('Not Found');
	err.status = 404;
	next(err);
});

// error handler
app.use(function(err, req, res) {
	// set locals, only providing error in development
	res.locals.message = err.message;
	res.locals.error = req.app.get('env') === 'development' ? err : {};

	// render the error page
	res.status(err.status || 500);
	res.render('error');
});

app.listen(app.get('port'));
console.log("" +
	"**********************************************\n" +
	"**                                          **\n" +
	"**          Fintan UI  is running           **\n" +
	"**          http://localhost:3009           **\n" +
	"**                                          **\n" +
	"**********************************************\n" +
	"");

console.log(`app: ${app.get('env')}`);

module.exports = app;
