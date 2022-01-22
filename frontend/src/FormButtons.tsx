import React from 'react';
import {Button} from "@material-ui/core";
import {useHistory} from "react-router";
import ButtonAppBar from "./Header";

type FormButton = {
    registerColor: 'default' | 'inherit' | 'primary' | 'secondary',
    findColor: 'default' | 'inherit' | 'primary' | 'secondary',
    listColor: 'default' | 'inherit' | 'primary' | 'secondary',
}

const FormButtons: React.FunctionComponent<FormButton> = ({registerColor, findColor, listColor}) => {
    const history = useHistory();

    const handleShowRegister = () => {
        history.push('/register');
    };

    const handleShowFind = () => {
        history.push('/find');
    };
    const handleShowList = () => {
        history.push('/list');
    };

    return (
        <>
            <ButtonAppBar/>
            <div className='action-buttons'>
                <Button href='' variant="contained" color={registerColor} onClick={handleShowRegister} className='action-buttons__register'>
                    Register Your Face
                </Button>
                <Button href='' variant="contained" color={findColor} onClick={handleShowFind} className='action-buttons__register'>
                    Find Your Face
                </Button>
                <Button href='' variant="contained" color={listColor} onClick={handleShowList}>
                    List Faces
                </Button>
            </div>
        </>
    )
};

export default FormButtons;