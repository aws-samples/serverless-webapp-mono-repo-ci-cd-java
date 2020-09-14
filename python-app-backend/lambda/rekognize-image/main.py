import base64
import json
import logging
import os

import boto3
from botocore.exceptions import ClientError
from aws_lambda_powertools import Tracer

logger = logging.getLogger()
rekognition = boto3.client('rekognition', region_name='eu-west-1')
dynamodb = boto3.client('dynamodb', region_name='eu-west-1')
collection = os.environ["CollectionName"]
table_name = os.environ["TableName"]
tracer = Tracer(service="searchImage")

@tracer.capture_lambda_handler
def lambda_handler(event, context):
    print(event)

    image = event['body']

    if image is None:
        return {
            "statusCode": 400,
            "headers": {
                'Access-Control-Allow-Origin': '*',
                'Access-Control-Allow-Methods': 'POST,GET'
            },
            "body": json.dumps({
                "message": "No image found in body. Pass base 64 encode image in the request body"
            })
        }

    decoded_image = base64.b64decode(image)
    try:
        response = rekognition.search_faces_by_image(
            CollectionId=collection,
            Image={'Bytes': decoded_image},
            MaxFaces=1,
            FaceMatchThreshold=90
        )

        print(response)

        faces = response.get('FaceMatches', [])

    except ClientError as e:
        logger.error("Bad image sent ?", exc_info=True)
        faces = []

    if len(faces) > 0:

        match = faces[0]
        print(match['Face']['FaceId'], match['Face']['Confidence'])

        face = dynamodb.get_item(
            TableName=table_name,
            Key={'RekognitionId': {'S': match['Face']['FaceId']}}
        )

        if 'Item' in face:
            return {
                "statusCode": 200,
                "headers": {
                    'Access-Control-Allow-Origin': '*',
                    'Access-Control-Allow-Methods': 'POST,GET'
                },
                "body": json.dumps({
                    "person_name": face['Item']['FullName']['S'],
                })
            }

    return {
        "statusCode": 200,
        "headers": {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'POST,GET'
        },
        "body": json.dumps({
            "message": "No match found in the record"
        })
    }
