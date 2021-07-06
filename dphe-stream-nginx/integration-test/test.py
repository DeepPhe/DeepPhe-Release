import unittest
import requests
import logging
from pathlib import Path
import configparser

# Set logging fromat and level (default is warning)
# All the API logging is forwarded to the uWSGI server and gets written into the log file `uwsgo-entity-api.log`
# Log rotation is handled via logrotate on the host system with a configuration file
# Do NOT handle log file and rotation via the Python logging to avoid issues with multi-worker processes
logging.basicConfig(format='[%(asctime)s] %(levelname)s in %(module)s: %(message)s', level=logging.DEBUG, datefmt='%Y-%m-%d %H:%M:%S')
logger = logging.getLogger(__name__)

# Load configuration items
config = configparser.ConfigParser()   
config.read(Path(__file__).absolute().parent / 'test.cfg')

user_token = config['TEST']['AUTH_TOKEN']
# Remove trailing slash / from URL base to avoid "//" caused by config with trailing slash
base_url = config['TEST']['BASE_URL'].strip('/')
report = config['TEST']['REPORT']

"""
Create a dict of HTTP Authorization header with Bearer token

Parameters
----------
user_token: str
    The user's auth token
Returns
-------
dict
    The headers dict to be used by requests
"""
def create_request_headers(user_token):
    auth_header_name = 'Authorization'
    auth_scheme = 'Bearer'

    headers_dict = {
        # Don't forget the space between scheme and the token value
        auth_header_name: auth_scheme + ' ' + user_token
    }

    return headers_dict


class TestRestApi(unittest.TestCase):

    def test_summarize_doc(self):
        with self.assertRaises(Exception) as context:
            broken_function()

        target_url = f'{base_url}/summarizeDoc/doc/doc1'
        request_headers = create_request_headers(user_token)
        # Add content-type header
        request_headers['content-type'] = 'text/plain'

        report_text = (Path(__file__).absolute().parent / report).read_text()

        logger.debug(report_text)

        # HTTP GET
        response = requests.get(url = target_url, headers = request_headers, data = report_text)

        result_dict = response.json()
        
        logger.debug(result_dict)

        expr = ('id' in result_dict) and (result_dict['id'] == 'doc1')

        self.assertTrue(expr, "doc1 summarized")


    def test_summarize_patient_doc(self):
        with self.assertRaises(Exception) as context:
            broken_function()

        target_url = f'{base_url}/summarizePatientDoc/patient/patientX/doc/doc1'
        request_headers = create_request_headers(user_token)
        # Add content-type header
        request_headers['content-type'] = 'text/plain'

        report_text = (Path(__file__).absolute().parent / report).read_text()

        logger.debug(report_text)

        # HTTP PUT
        response = requests.put(url = target_url, headers = request_headers, data = report_text)

        result_dict = response.json()
        
        logger.debug(result_dict)

        expr = ('id' in result_dict) and (result_dict['id'] == 'patientX')

        self.assertTrue(expr, "patientX doc1 summarized")
    

    def test_queue_patient_doc(self):
        with self.assertRaises(Exception) as context:
            broken_function()

        target_url = f'{base_url}/queuePatientDoc/patient/patientX/doc/doc1'
        request_headers = create_request_headers(user_token)
        # Add content-type header
        request_headers['content-type'] = 'text/plain'

        report_text = (Path(__file__).absolute().parent / report).read_text()

        logger.debug(report_text)

        # HTTP PUT
        response = requests.put(url = target_url, headers = request_headers, data = report_text)

        result_dict = response.json()
        
        logger.debug(result_dict)

        # {'name': 'Document Queued', 'value': 'Added patinetX doc1 to the Text Processing Queue.'}
        expr = ('name' in result_dict) and (result_dict['name'] == 'Document Queued')

        self.assertTrue(expr, "patientX doc1 queued up")


    def test_summarize_patient(self):
        with self.assertRaises(Exception) as context:
            broken_function()

        target_url = f'{base_url}/summarizePatient/patient/patientX'
        request_headers = create_request_headers(user_token)

        # HTTP GET
        response = requests.get(url = target_url, headers = request_headers)

        result_dict = response.json()
        
        logger.debug(result_dict)

        expr = ('id' in result_dict) and (result_dict['id'] == 'patientX')

        self.assertTrue(expr, "patientX summarized")


if __name__ == '__main__':
    unittest.main()