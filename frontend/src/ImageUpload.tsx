import React, {useState} from 'react';
import ImageUploader from "react-images-upload";
import {Button, CircularProgress, TextField} from "@material-ui/core";

import {GLOBAL_CONSTANTS} from "./GlobalConstants";
import FormButtons from "./FormButtons";

type DataToGetUrl = {
    name: string,
    type: string,
    extension: string,
}

type Result = {
    uploadURL: string,
    fileName: string,
}

const ImageUpload: React.FunctionComponent = () => {
    const [picture, setPicture] = useState<File>();
    const [name, setName] = useState<string>('');
    const [message, setMessage] = useState<string>();
    const [loading, setLoading] = useState<boolean>();

    const handleImageChange = (picture:File[]) => {
        setMessage(undefined);
        setLoading(false)
        setPicture(picture[0]);
    };

    const submitForm = () => {
        setMessage(undefined);
        setLoading(true)

        if(picture) {
            const dataToGetUrl: DataToGetUrl = {
                name: name,
                type: picture.type,
                extension: picture.type.split('/')[1]
            };
            getUrl(dataToGetUrl);
        }
    };

    const getUrl = (dataToGetUrl: DataToGetUrl) => {
        fetch(`${GLOBAL_CONSTANTS.UPLOAD_URL}?content-type=${dataToGetUrl.type}&file-extension=.${dataToGetUrl.extension}&person-name=${dataToGetUrl.name}`)
            .then(res => res.json())
            .then(
                (result) => {
                    submitImage(result);
                },
                (error) => {
                    console.log(error);
                }
            ).catch(err => {
                setMessage('Upload Failed');
                setLoading(false)
            }
        )
    };

    const submitImage = (result: Result) => {
        const putMethod = {
            method: 'PUT',
            body: picture,
        };

        if(result.uploadURL) {
            fetch(result.uploadURL, putMethod)
                .then(response => {
                    setLoading(false)
                    if(response.status === 200 && response.statusText === 'OK') {
                        setMessage('Upload Successful');
                    } else {
                        setMessage('Upload Failed');
                    }
                })
                .catch(err => {
                    setMessage('Upload Failed');
                    setLoading(false)
                });
        } else {
            setMessage('Upload Failed');
            setLoading(false)
        }
    };

    return (
        <div className='image-form'>
            <FormButtons registerColor='primary' findColor='default'/>
            <TextField
                autoFocus
                id="name"
                label="Name"
                variant="outlined"
                value={name}
                onChange={(e) => setName(e.target.value)}/>
            <ImageUploader
                withIcon={true}
                onChange={handleImageChange}
                imgExtension={[".jpg", ".gif", ".png", ".jpeg"]}
                maxFileSize={5242880}
                withPreview={true}
                singleImage={true}
                label='Max file size: 5mb, accepted: jpg | gif | png | jpeg'
            />
            {message && <div className='message'>{message}</div>}
            {loading && <CircularProgress size={24}/>}
            <Button href='' variant="contained" color="primary" onClick={submitForm} className='submit-button' disabled={!picture || loading}>
                Upload
            </Button>


        </div>
    );
};

export default ImageUpload;