import json
import os
import uuid

import boto3
from aws_lambda_powertools import Tracer

s3_client = boto3.client('s3')
s3_bucket = os.environ["UploadBucket"]
tracer = Tracer(service="uploadUrlGenerator")


@tracer.capture_lambda_handler
def lambda_handler(event, context):
    content_type = event.get('queryStringParameters', {}).get('content-type', '')
    file_extension = event.get('queryStringParameters', {}).get('file-extension', '')

    if content_type == '' or file_extension == '':
        return {
            "statusCode": 400,
            "body": json.dumps({
                "message": "Both content-type and file-extension need to passed as query param!"
            })
        }

    person_name = event['queryStringParameters'].get('person-name', '')

    metadata = {
        'fullname': person_name
    }

    file_name = str(uuid.uuid4()) + file_extension

    s3_image_path = 'index/static/' + file_name

    pre_signed_image_url = s3_client.generate_presigned_url('put_object',
                                                           Params={'Bucket': s3_bucket,
                                                                   'Key': s3_image_path,
                                                                   'ContentType': content_type,
                                                                   'Metadata': metadata},
                                                           ExpiresIn=60)

    return {
        "statusCode": 200,
        "headers": {
            "Access-Control-Allow-Origin": "*"
        },
        "body": json.dumps({
            "uploadURL": pre_signed_image_url,
            "fileName": file_name
        })
    }
