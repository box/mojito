import React from "react";
import ImageClient from "../../sdk/ImageClient";
import { Modal } from "react-bootstrap";
import uuidv4 from "uuid/v4";

class ScreenshotDropzonePage extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            dragging: false,
            items: [], // { id, fileName, preview, uuid, uploading, error }
            zoomSrc: null
        };
        this.supportedImageExtensionsSet = new Set([".png", ".jpg", ".jpeg"]);
    }

    onDragOver = (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (!this.state.dragging) this.setState({dragging: true});
    }

    onDragLeave = (e) => {
        e.preventDefault();
        e.stopPropagation();
        this.setState({dragging: false});
    }

    onDrop = (e) => {
        e.preventDefault();
        e.stopPropagation();
        this.setState({dragging: false});
        const files = e.dataTransfer && e.dataTransfer.files ? e.dataTransfer.files : [];
        if (files.length > 0) {
            Array.from(files).forEach(f => this.processFile(f));
        }
    }

    onFilePicked = (e) => {
        const files = e.target.files;
        if (files && files.length > 0) {
            Array.from(files).forEach(f => this.processFile(f));
            // reset input to allow selecting same files again if needed
            e.target.value = null;
        }
    }

    isImageExtensionSupported(name) {
        const idx = name.lastIndexOf(".");
        const ext = idx >= 0 ? name.substring(idx).toLowerCase() : "";
        return this.supportedImageExtensionsSet.has(ext);
    }

    processFile(file) {
        const id = `${Date.now()}-${Math.random()}`;

        if (!this.isImageExtensionSupported(file.name)) {
            const item = { id, fileName: file.name, preview: null, uuid: null, uploading: false, error: `${file.name} is not in .png, .jpg or .jpeg format.` };
            this.setState(prev => ({ items: [item, ...prev.items] }));
            return;
        }

        const item = { id, fileName: file.name, preview: null, uuid: null, uploading: true, error: null };
        this.setState(prev => ({ items: [item, ...prev.items] }));

        const readerPreview = new FileReader();
        const readerUpload = new FileReader();

        readerPreview.onloadend = () => {
            this.updateItem(id, { preview: readerPreview.result });
        };

        readerUpload.onloadend = () => {
            const generatedUuid = uuidv4();
            ImageClient.uploadImage(generatedUuid, readerUpload.result)
                .then(() => {
                    this.updateItem(id, { uploading: false, uuid: generatedUuid });
                })
                .catch(() => {
                    this.updateItem(id, { uploading: false, error: "Couldn't upload image" });
                });
        };

        readerPreview.readAsDataURL(file);
        readerUpload.readAsArrayBuffer(file);
    }

    updateItem(id, patch) {
        this.setState(prev => ({
            items: prev.items.map(it => it.id === id ? { ...it, ...patch } : it)
        }));
    }

    copyToClipboard = (text) => {
        if (!text) return;
        try {
            navigator.clipboard && navigator.clipboard.writeText(text);
        } catch (e) {
            // ignore
        }
    }

    render() {
        const { dragging, items } = this.state;
        const anyUploading = items.some(it => it.uploading);

        const dropzoneStyle = {
            border: dragging ? '2px solid #337ab7' : '2px dashed #bbb',
            borderRadius: 8,
            padding: 40,
            textAlign: 'center',
            color: '#666',
            cursor: 'pointer',
            maxWidth: 720,
            margin: '40px auto'
        };

        const hintStyle = { marginTop: 10, color: '#999', fontSize: 12 };

        return (
            <div>
                <div
                    style={dropzoneStyle}
                    onDragOver={this.onDragOver}
                    onDragLeave={this.onDragLeave}
                    onDrop={this.onDrop}
                    onClick={() => this.fileInput && this.fileInput.click()}
                >
                    <div>
                        {anyUploading ? 'Uploading…' : (dragging ? 'Drop images to upload' : 'Drag & drop images here or click to select')}
                    </div>
                    <div style={hintStyle}>
                        Accepted: .png, .jpg, .jpeg
                    </div>
                    <input
                        ref={r => this.fileInput = r}
                        type="file"
                        accept="image/png,image/jpeg"
                        multiple
                        style={{display: 'none'}}
                        onChange={this.onFilePicked}
                    />
                </div>

                {items.length > 0 && (
                    <div style={{
                        maxWidth: 1200,
                        margin: '0 auto',
                        display: 'flex',
                        flexWrap: 'wrap',
                        gap: 16
                    }}>
                        {items.map(it => (
                            <div key={it.id} style={{
                                width: 300,
                                border: '1px solid #eee',
                                borderRadius: 6,
                                padding: 10,
                                boxShadow: '0 1px 2px rgba(0,0,0,0.03)'
                            }}>
                                <div style={{
                                    width: '100%',
                                    height: 200,
                                    background: '#fafafa',
                                    borderRadius: 4,
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    overflow: 'hidden',
                                    position: 'relative'
                                }}>
                                    {it.preview && (
                                        <img
                                            src={it.preview}
                                            alt={it.fileName}
                                            style={{maxWidth: '100%', maxHeight: '100%', cursor: 'zoom-in'}}
                                            onClick={() => this.setState({ zoomSrc: it.preview })}
                                        />
                                    )}
                                    {it.uploading && (
                                        <div style={{
                                            position: 'absolute',
                                            bottom: 6,
                                            right: 6,
                                            background: 'rgba(255,255,255,0.8)',
                                            borderRadius: 3,
                                            padding: '2px 6px',
                                            fontSize: 12
                                        }}>Uploading…</div>
                                    )}
                                </div>
                                <div style={{marginTop: 8, wordBreak: 'break-all'}}>
                                    <div style={{fontSize: 12, color: '#999'}}>{it.fileName}</div>
                                    {it.uuid && (
                                        <div style={{marginTop: 6}}>
                                            <div style={{
                                                fontFamily: 'monospace',
                                                fontSize: 12,
                                                whiteSpace: 'nowrap',
                                                overflow: 'hidden',
                                                textOverflow: 'clip',
                                                textAlign: 'left'
                                            }}>{`s:${it.uuid}`}</div>
                                            <div style={{textAlign: 'center', marginTop: 6}}>
                                                <button className="btn btn-xs btn-default" onClick={() => this.copyToClipboard(`s:${it.uuid}`)}>Copy</button>
                                            </div>
                                        </div>
                                    )}
                                    {it.error && (
                                        <div className="alert alert-danger" style={{marginTop: 8, padding: '4px 8px'}}>{it.error}</div>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                )}

                <Modal className="screenshot-modal" show={!!this.state.zoomSrc} onHide={() => this.setState({ zoomSrc: null })}>
                    <Modal.Header closeButton>
                        <Modal.Title>Preview</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        {this.state.zoomSrc && (
                            <img
                                src={this.state.zoomSrc}
                                alt="preview"
                                style={{
                                    display: 'block',
                                    maxWidth: '100%',
                                    height: 'auto',
                                    maxHeight: '80vh',
                                    margin: '0 auto'
                                }}
                                onClick={() => this.setState({ zoomSrc: null })}
                            />
                        )}
                    </Modal.Body>
                </Modal>
            </div>
        );
    }
}

export default ScreenshotDropzonePage;
