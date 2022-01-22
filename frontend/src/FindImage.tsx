import React, {useContext, useState} from 'react';
import ImageUploader from "react-images-upload";
import {Button, CircularProgress} from "@material-ui/core";
import {GLOBAL_CONSTANTS} from "./GlobalConstants";
import FormButtons from "./FormButtons";
import {BackendContext} from "./App";

const FindImage: React.FunctionComponent = () => {
    const [pictureDataUrl, setPictureDataUrl] = useState<string>('');
    const [name, setName] = useState<string>();
    const [message, setMessage] = useState<string>();
    const [loading, setLoading] = useState<boolean>();
    const [backend] = useContext(BackendContext);

    const handleImageChange = (picture:File[], pictureDataUrl: string[]) => {
        setPictureDataUrl(pictureDataUrl[0]);
        setLoading(false)
        resetMessage();
    };

    const resetMessage = () => {
        setName(undefined);
        setLoading(false)
        setMessage(undefined);
    };

    const findImage = () => {
        resetMessage();
        setLoading(true)
        if(pictureDataUrl) {
            fetch(GLOBAL_CONSTANTS.get(backend)?.FIND_IMAGE as string, {
                method: 'POST',
                body: pictureDataUrl.split('base64,')[1],
            })
            .then(response => response.json())
            .then(result => {
                setLoading(false)
                if(result.person_name) {
                    setName(result.person_name);
                } else {
                    setMessage(result.message);
                }
            })
            .catch(error => {
                setLoading(false)
                console.error('Error:', error);
            });
        }
    };

    return (
        <div className='image-form'>
            <FormButtons registerColor='default' findColor='primary' listColor='default'/>
            <ImageUploader
                withIcon={true}
                onChange={handleImageChange}
                imgExtension={[".jpg", ".gif", ".png", ".jpeg"]}
                maxFileSize={5242880}
                withPreview={true}
                singleImage={true}
                label='Max file size: 5mb, accepted: jpg | gif | png | jpeg'
            />
            {name && <div className='message'>Hi, {name}</div>}
            {message && <div className='message'>{message}</div>}
            {loading && <CircularProgress size={24}/>}
            <Button href='' variant="contained" color="primary" onClick={findImage} className='submit-button' disabled={!pictureDataUrl || loading}>
                Find
            </Button>
        </div>
    );
};

export default FindImage;