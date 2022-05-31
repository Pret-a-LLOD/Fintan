import subprocess
import os
import glob
import gzip

from flask import Flask, request, jsonify, send_file, redirect
from flask_swagger_ui import get_swaggerui_blueprint
from flask_cors import CORS
from werkzeug.utils import secure_filename


def load_pipelines(path):
    return list(sorted(set(os.path.split(pipeline)[-1].replace('.json', '') for pipeline in glob.glob(path))))


path_pipelines = os.environ.get('FINTAN_PIPELINES', './pipelines/')
path_data = os.environ.get('FINTAN_DATA', './')
path_uploads = os.environ.get('FINTAN_UPLOADS', './')
path_fintan = os.environ.get('FINTAN_PATH', '../../fintan-backend/')
path_jar = os.environ.get('FINTAN_JAR', 'fintan-backend.jar')
java_run = ['java', '-Dfile.encoding=UTF8', '-jar', os.path.join(path_fintan, path_jar)]

upload_extensions = {'.json', '.ttl', '.n3', '.rdf', '.gz', '.zip', '.txt', '.yaml', '.sparql', '.rq'}

tmpl_pipelines = os.path.join(path_pipelines, '{}.json')

pipelines = load_pipelines(tmpl_pipelines.format('*'))

app = Flask(__name__)
CORS(app)
swagger_ui = get_swaggerui_blueprint('/api/docs', '/static/openapi.yaml')
app.register_blueprint(swagger_ui)


pipeline_validators = {
    'application/json': lambda request: request.json is not None and 'text' in request.json,
    'application/gzip': lambda request: request.data is not None,
    'text/plain': lambda request: request.data is not None
}

pipeline_content_getters = {
    'application/json': lambda request: request.json.get('text', ''),
    'application/gzip': lambda request: gzip.decompress(request.data).decode('utf-8'),
    'text/plain': lambda request: request.data.decode('utf-8')
}


@app.route('/api/pipelines')
@app.route('/api/pipelines/')
def pipeline_list():
    return jsonify({'pipelines': pipelines})


@app.route('/api/files')
@app.route('/api/files/<filename>')
def files(filename=None):
    data_files = os.listdir(path_data)
    if not filename:
        return jsonify({'files': sorted(data_files)})
    return open(os.path.join(path_data, filename)).read() if filename in data_files else (jsonify({'error': 'File not found'}), 404)


@app.route('/api/upload', methods=['POST', 'PUT'])
def upload():
    if 'file' not in request.files or not request.files['file'] or not request.files['file'].filename:
        return jsonify({'error': 'No file provided'}), 400

    upload_file = request.files['file']
    filename = secure_filename(os.path.split(upload_file.filename)[-1])
    if os.path.splitext(filename)[1].lower() not in upload_extensions:
        return jsonify({'error': 'Extension not supported'}), 400

    pipeline_path = request.values.get('pipeline', '.')
    if not os.path.exists(pipeline_path):
        os.mkdir(pipeline_path)
        os.mkdir(os.path.join(pipeline_path, 'data'))

    upload_type = request.values.get('type', 'data')
    upload_file.save(os.path.join(os.path.join(os.path.join(path_uploads, pipeline_path), 'data') if upload_type == 'data' else path_pipelines, filename))

    if upload_type != 'data':
        global pipelines
        pipelines = load_pipelines(tmpl_pipelines.format('*'))

    return jsonify({'success': 'File successfully uploaded'}), 200


# TODO: do we want to save the file or give it through the STDIN?
@app.route('/api/run', methods=['POST'])
@app.route('/api/run/<pipeline>', methods=['POST'])
def execute_pipeline(pipeline=None):
    if not pipeline:
        if not pipelines:
            return jsonify({'error': 'No pipelines found, upload or ship one with the container'}), 400
        if len(pipelines) > 1:
            return jsonify({'error': 'Multiple pipelines found, pipeline argument must be specified explicitly'}), 400
        pipeline = next(iter(pipelines))
    if pipeline not in pipelines:
        return jsonify({'error': 'Pipeline not found'}), 404

    # if not os.path.exists(path_run_sh):
    #    return 'Executable not found', 500

    content_type = request.mimetype
    if request.data and content_type not in pipeline_validators:
        return jsonify({'error:': 'Unsupported content-type for input data'}), 415

    params = request.values.get('params')

    print(path_fintan)
    print(os.path.realpath(tmpl_pipelines.format(pipeline)))
    content = pipeline_content_getters[content_type](request) if request.data else ''
    ret_val = subprocess.run(java_run +
                             ["-c", os.path.realpath(tmpl_pipelines.format(pipeline))] +
                             (["-p", params] if params else []),
                             capture_output=True,
                             input=content + '\n',
                             text=True,
                             cwd=path_data,
                             encoding='utf-8')

    print(ret_val.stderr)
    return jsonify({'result': ret_val.stdout})


@app.route('/static/openapi.yaml')
def openapi_spec():
    return send_file('/fintan/openapi.yaml' if os.path.exists('/fintan/openapi.yaml') else 'openapi.yaml', cache_timeout=-1)


@app.route('/', methods=['GET'])
def index():
    return redirect('/api/docs')


if __name__ == '__main__':
    app.run(debug=True, port=8080)
