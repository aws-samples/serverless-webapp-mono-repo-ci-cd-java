import React from 'react';
import {Button} from "@material-ui/core";
import {useHistory} from "react-router";
import ButtonAppBar from "./Header";

type FormButtons = {
    registerColor: 'default' | 'inherit' | 'primary' | 'secondary',
    findColor: 'default' | 'inherit' | 'primary' | 'secondary',
}

const FormButtons: React.FunctionComponent<FormButtons> = ({registerColor, findColor}) => {
    const history = useHistory();

    const handleShowRegister = () => {
        history.push('/register');
    };

    const handleShowFind = () => {
        history.push('/find');
    };

    return (
        <>
            <ButtonAppBar/>
            <div className='action-buttons'>
                <Button href='' variant="contained" color={registerColor} onClick={handleShowRegister} className='action-buttons__register'>
                    Register Your Face
                </Button>
                <Button href='' variant="contained" color={findColor} onClick={handleShowFind}>
                    Find Your Face
                </Button>
            </div>
        </>
    )
};

export default FormButtons;