type Config = {
    FIND_IMAGE: string,
    UPLOAD_URL: string,
    LIST_FACES: string,
}

export const GLOBAL_CONSTANTS = new Map<string, Config>();

GLOBAL_CONSTANTS.set('JAVA', {
        'FIND_IMAGE': `https://m7ld6m2q09.execute-api.eu-west-1.amazonaws.com/Prod/recognise`,
        'UPLOAD_URL': `https://m7ld6m2q09.execute-api.eu-west-1.amazonaws.com/Prod/upload-url`,
        'LIST_FACES': `https://m7ld6m2q09.execute-api.eu-west-1.amazonaws.com/Prod/list-faces`,
    });

GLOBAL_CONSTANTS.set('PYTHON', {
    'FIND_IMAGE': `https://2rb53n6axc.execute-api.eu-west-1.amazonaws.com/Prod/recognise`,
    'UPLOAD_URL': `https://2rb53n6axc.execute-api.eu-west-1.amazonaws.com/Prod/upload-url`,
    'LIST_FACES': `https://2rb53n6axc.execute-api.eu-west-1.amazonaws.com/Prod/list-faces`,
});

export const Links_List = [
    {label: 'Source code for the project', link: 'https://github.com/aws-samples/serverless-webapp-mono-repo-ci-cd-java'},
    {label: 'Architecture diagram', link: '/serverless-webapp-mono-repo-ci-cd-java.png'},
    {label: 'Amazon Rekognition', link: 'https://aws.amazon.com/rekognition/'},
    {label: 'AWS Serverless Application Model', link: 'https://aws.amazon.com/serverless/sam/'},
    {label: 'AWS Cloud Development Kit (AWS CDK)', link: 'https://docs.aws.amazon.com/cdk/latest/guide/work-with-cdk-java.html'},
    {label: 'Learn more about AWS developer tools services?', link: 'https://aws.amazon.com/products/developer-tools/'},
    {label: 'Connect with me @agrawalpankaj16', link: 'https://twitter.com/agrawalpankaj16'},
];
