from __future__ import print_function
from aws_lambda_powertools import Tracer

import urllib.parse
import os
import boto3


print('Loading function')

tracer = Tracer(service="indexImage")
dynamodb = boto3.client('dynamodb')
s3 = boto3.client('s3')
rekognition = boto3.client('rekognition')
collection_id = os.environ['CollectionId']
table_name = os.environ['TableName']


# --------------- Helper Functions ------------------

def index_faces(bucket, key):
    response = rekognition.index_faces(
        Image={"S3Object":
                   {"Bucket": bucket,
                    "Name": key}},
        CollectionId=collection_id)
    print(response)
    return response


def update_index(tableName, faceId, fullName):
    response = dynamodb.put_item(
        TableName=tableName,
        Item={
            'RekognitionId': {'S': faceId},
            'FullName': {'S': fullName}
        }
    )

    print(response)


# --------------- Main handler ------------------

@tracer.capture_lambda_handler
def lambda_handler(event, context):
    # Get the object from the event
    bucket = event['Records'][0]['s3']['bucket']['name']
    key = urllib.parse.unquote_plus(event['Records'][0]['s3']['object']['key'])

    try:

        # Calls Amazon Rekognition IndexFaces API to detect faces in S3 object
        # to index faces into specified collection

        response = index_faces(bucket, key)

        # Commit faceId and full name object metadata to DynamoDB

        if response['ResponseMetadata']['HTTPStatusCode'] == 200:
            faceId = response['FaceRecords'][0]['Face']['FaceId']

            ret = s3.head_object(Bucket=bucket, Key=key)
            person_full_name = ret['Metadata']['fullname']

            tracer.put_metadata(key="full_name", value=person_full_name)

            update_index(table_name, faceId, person_full_name)

        # Print response to console.
        print(response)

        return response
    except Exception as e:
        print(e)
        print("Error processing {} from bucket {}. ".format(key, bucket))
        raise e
