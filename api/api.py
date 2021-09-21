import subprocess
import os
import glob
import gzip

from flask import Flask, request, jsonify, send_file, redirect
from flask_swagger_ui import get_swaggerui_blueprint
from werkzeug.utils import secure_filename


def load_pipelines(path):
    return set(os.path.split(pipeline)[-1].replace('.json', '') for pipeline in glob.glob(path))


path_pipelines = os.environ.get('FINTAN_PIPELINES', './pipelines/')
path_data = os.environ.get('FINTAN_DATA', './data/')
path_fintan = os.environ.get('FINTAN_PATH', './fintan-backend/')
path_run_sh = os.path.join(path_fintan, 'run.sh')

upload_extensions = {'.json', '.ttl', '.n3', '.rdf', '.gz', '.zip', '.txt', '.yaml'}

tmpl_pipelines = path_pipelines + '{}.json'

pipelines = load_pipelines(tmpl_pipelines.format('*'))

app = Flask(__name__)
swagger_ui = get_swaggerui_blueprint('/api/docs', '/static/openapi.yaml')
app.register_blueprint(swagger_ui)


pipeline_validators = {
    'application/json': lambda request: request.json is not None and 'text' in request.json,
    'application/gzip': lambda request: request.data is not None,
    'text/plain': lambda request: request.data is not None
}

pipeline_content_getters = {
    'application/json': lambda request: request.json['text'],
    'application/gzip': lambda request: gzip.decompress(request.data).decode('utf-8'),
    'text/plain': lambda request: request.data.decode('utf-8')
}


@app.route('/api/upload', methods=['POST', 'PUT'])
@app.route('/api/upload/', methods=['POST', 'PUT'])
def upload():
    if 'file' not in request.files or not request.files['file'] or not request.files['file'].filename:
        return jsonify({'error': 'No file provided'}), 400

    upload_file = request.files['file']
    filename = secure_filename(upload_file.filename)
    if os.path.splitext(filename)[1].lower() not in upload_extensions:
        return jsonify({'error': 'Extension not supported'}), 400

    upload_type = request.values.get('type', 'data')
    upload_file.save(os.path.join(path_data if upload_type == 'data' else path_pipelines, filename))

    return jsonify({'success': 'File successfully uploaded'}), 200


# TODO: do we want to save the file or give it through the STDIN?
@app.route('/api/run', methods=['POST'])
@app.route('/api/run/<pipeline>', methods=['POST'])
@app.route('/api/run/<pipeline>/', methods=['POST'])
def execute_pipeline(pipeline=None):
    if not pipeline:
        if len(pipelines) > 1:
            return jsonify({'error': 'Multiple pipelines found, pipeline argument must be specified explicitly'}), 400
        pipeline = next(iter(pipelines))
    if pipeline not in pipelines:
        return jsonify({'error': 'Pipeline not found'}), 404

    # if not os.path.exists(path_run_sh):
    #    return 'Executable not found', 500

    content_type = request.mimetype
    if content_type not in pipeline_validators:
        return jsonify({'error:': 'Unsupported content-type for input data'}), 415

    params = request.values.get('params')

    content = pipeline_content_getters[content_type](request)
    ret_val = subprocess.run([path_run_sh,
                              "-c", os.path.realpath(tmpl_pipelines.format(pipeline))] +
                             (["-p", params] if params else []),
                             capture_output=True,
                             input=content + '\n',
                             text=True,
                             cwd=path_fintan,
                             encoding='utf-8')

    print(ret_val.stderr)
    return ret_val.stdout


@app.route('/static/openapi.yaml')
def openapi_spec():
    return send_file('/fintan/openapi.yaml' if os.path.exists('/fintan/openapi.yaml') else 'openapi.yaml', cache_timeout=-1)


@app.route('/', methods=['GET'])
def index():
    return redirect('/api/docs')


if __name__ == '__main__':
    app.run(debug=True)
