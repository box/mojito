import React from "react"
import PropTypes from "prop-types";

class ImageUpload extends React.Component {
    static propTypes = {
        "onChooseImageClick": PropTypes.func.isRequired
    };
    // constructor(props) {
    //     super(props);
    //     this.state = {file: '',imagePreviewUrl: ''};
    // }

    // _handleSubmit(e) {
    //     e.preventDefault();
    //     // TODO: do something with -> this.state.file
    //     console.log('handle uploading-', this.state.file);
    // }

    handleImageChange(e) {
        e.preventDefault();

        let reader = new FileReader();
        let file = e.target.files[0];

        reader.onloadend = () => {
            this.props.onChooseImageClick({
                file: file,
                imagePreviewUrl: reader.result
            });
        };

        reader.readAsDataURL(file)
    }

    render() {

        return (
            <div className="previewComponent">
                <form>
                    <input className="fileInput"
                           type="file"
                           onChange={(e)=>this.handleImageChange(e)}
                           webkitdirectory
                    />
                </form>
            </div>
        )
    }
}

export default ImageUpload;
