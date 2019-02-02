import React from "react"
import PropTypes from "prop-types";

class ImageUpload extends React.Component {
    static propTypes = {
        "onChooseImageClick": PropTypes.func.isRequired
    };

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

        reader.readAsArrayBuffer(file)
    }

    render() {

        return (
            <div>
                <form>
                    <input className="fileInput"
                           type="file"
                           onChange={(e)=>this.handleImageChange(e)}
                           accept="image/*"
                    />
                </form>
            </div>
        )
    }
}

export default ImageUpload;
