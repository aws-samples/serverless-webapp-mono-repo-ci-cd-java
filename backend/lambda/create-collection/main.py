import boto3
import json
import cfnresponse
import logging
import traceback


def lambda_handler(event, context):
    print('received event:' + json.dumps(event, indent=2))

    rekognition = boto3.client('rekognition')
    logger = logging.getLogger()

    if event['RequestType'] == 'Delete':
        try:
            ret = rekognition.delete_collection(CollectionId=str(event['ResourceProperties']['CollectionName']))
            if ret['ResponseMetadata']['HTTPStatusCode'] == 200:
                print('Resource deleted')
                cfnresponse.send(event, context, "SUCCESS", {})
            return
        except:
            logger.error("error: {0}".format(traceback.format_exc()))
            cfnresponse.send(event, context, "FAILED", {})
    else:
        try:
            ret = rekognition.create_collection(CollectionId=str(event['ResourceProperties']['CollectionName']))
            if ret['ResponseMetadata']['HTTPStatusCode'] == 200:
                print('Resource created')
                cfnresponse.send(event, context, "SUCCESS", {})
        except:
            logger.error("error: {0}".format(traceback.format_exc()))
            cfnresponse.send(event, context, "FAILED", {})
