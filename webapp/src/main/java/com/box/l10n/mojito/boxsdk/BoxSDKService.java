package com.box.l10n.mojito.boxsdk;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.box.sdk.BoxSharedLink;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** @author jaurambault */
@Component
public class BoxSDKService {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(BoxSDKService.class);

  @Autowired BoxAPIConnectionProvider boxAPIConnectionProvider;

  @Autowired BoxSDKServiceConfigProvider boxSDKServiceConfigProvider;

  public BoxSDKServiceConfig getBoxSDKServiceConfig() throws BoxSDKServiceException {
    return boxSDKServiceConfigProvider.getConfig();
  }

  public BoxAPIConnection getBoxAPIConnection() throws BoxSDKServiceException {
    return boxAPIConnectionProvider.getConnection();
  }

  /**
   * Returns the Box root folder
   *
   * @return The root folder for the current profile (or null if not found)
   * @throws BoxSDKServiceException When an error occurred while retrieving the folder
   */
  public BoxFolder getRootFolder() throws BoxSDKServiceException {
    try {
      return new BoxFolder(getBoxAPIConnection(), getBoxSDKServiceConfig().getRootFolderId());
    } catch (BoxAPIException e) {
      throw new BoxSDKServiceException("Can't retrieve the root folder", e);
    }
  }

  /**
   * Creates a shared folder inside the drop folder
   *
   * @param folderName The name of the shared folder to create
   * @return The created shared folder
   * @throws BoxSDKServiceException When an error occurred while creating the folder
   */
  public BoxFolder createSharedFolder(String folderName) throws BoxSDKServiceException {
    return createSharedFolder(folderName, getBoxSDKServiceConfig().getRootFolderId());
  }

  /**
   * Creates a shared folder in the given parent folder
   *
   * @param folderName The name of the shared folder to create
   * @param parentId The ID of the parent folder where the folder should be created
   * @return The created shared folder
   * @throws BoxSDKServiceException When an error occurred while creating the folder
   */
  public BoxFolder createSharedFolder(String folderName, String parentId)
      throws BoxSDKServiceException {

    BoxFolder createFolder = createFolder(folderName, parentId);

    try {
      createFolder.createSharedLink(BoxSharedLink.Access.OPEN, null, null);
      return createFolder;
    } catch (BoxAPIException e) {
      throw new BoxSDKServiceException(
          "Can't create shared link for directory: " + createFolder.getID(), e);
    }
  }

  /**
   * Creates a folder inside the drop folder
   *
   * @param folderName The name of the folder to create
   * @return The created folder
   * @throws BoxSDKServiceException When an error occurred while creating the folder
   */
  public BoxFolder createFolderUnderRoot(String folderName) throws BoxSDKServiceException {
    return createFolder(folderName, getBoxSDKServiceConfig().getRootFolderId());
  }

  /**
   * Creates a folder inside the given parent folder
   *
   * @param folderName The name of the folder to create
   * @param parentId The ID of the parent folder where the folder should be created
   * @return The created folder
   * @throws BoxSDKServiceException When an error occurred while creating the folder
   */
  public BoxFolder createFolder(String folderName, String parentId) throws BoxSDKServiceException {
    try {
      BoxFolder parentFolder = new BoxFolder(getBoxAPIConnection(), parentId);
      BoxFolder.Info createFolderInfo = parentFolder.createFolder(folderName);
      logger.debug(
          "created: " + createFolderInfo.getID() + ", name: " + createFolderInfo.getName());
      return createFolderInfo.getResource();
    } catch (BoxAPIException e) {
      throw new BoxSDKServiceException("Can't create folder: " + folderName, e);
    }
  }

  /**
   * Uploads a file to the given folder.
   *
   * <p>Creates new file if needed or update existing file.
   *
   * @param folderId id of the folder where the file should be uploaded
   * @param filename The name of the file to be uploaded
   * @param filecontent The content of the file to be uploaded
   * @return The uploaded file
   * @throws BoxSDKServiceException When an error occurred while uploading the file
   */
  public BoxFile uploadFile(String folderId, String filename, String filecontent)
      throws BoxSDKServiceException {

    try {

      BoxFile uploadFile = getFileByName(folderId, filename);

      if (uploadFile == null) {
        logger.debug("Upload a new file named: {} to folder: {}", filename, folderId);
        BoxFolder boxFolder = new BoxFolder(getBoxAPIConnection(), folderId);

        BoxFile.Info uploadFileInfo =
            boxFolder.uploadFile(
                IOUtils.toInputStream(filecontent, StandardCharsets.UTF_8), filename);
        uploadFile = uploadFileInfo.getResource();

        logger.debug("Uploaded new file, id: " + uploadFile.getID() + ", name: " + filename);
      } else {
        logger.debug("Upload a new version of file named: {} to folder: {}", filename, folderId);
        uploadFile.uploadVersion(IOUtils.toInputStream(filecontent, StandardCharsets.UTF_8));

        logger.debug(
            "Uploaded new version of file, id: " + uploadFile.getID() + ", name: " + filename);
      }

      return uploadFile;

    } catch (BoxAPIException e) {
      String msg = "Can't upload file: " + filename + ", in folder id: " + folderId;
      logger.error(msg, e);
      throw new BoxSDKServiceException(msg, e);
    }
  }

  /**
   * Gets a file by name in a folder.
   *
   * @param folderId the folder id in which to look for the file
   * @param filename the filename
   * @return the file if exists else {@link null}.
   * @throws BoxSDKServiceException
   */
  private BoxFile getFileByName(String folderId, String filename) throws BoxSDKServiceException {

    Preconditions.checkNotNull(filename, "filename must not be null");

    BoxFile boxFile = null;

    for (BoxFile listFile : listFiles(folderId)) {
      if (filename.equals(listFile.getInfo().getName())) {
        boxFile = listFile;
        break;
      }
    }

    return boxFile;
  }

  /**
   * Lists the files in a folder.
   *
   * @param folderId The folder id
   * @return A list of files (empty list if no files)
   * @throws BoxSDKServiceException When an error occurred while listing the files
   */
  public List<BoxFile> listFiles(String folderId) throws BoxSDKServiceException {

    try {
      BoxFolder folder = new BoxFolder(getBoxAPIConnection(), folderId);

      List<BoxFile> files = new ArrayList<>();

      for (BoxItem.Info itemInfo : folder) {
        if (itemInfo instanceof BoxFile.Info) {
          BoxFile.Info fileInfo = (BoxFile.Info) itemInfo;
          files.add(fileInfo.getResource());
        }
      }

      return files;
    } catch (BoxAPIException e) {
      String msg = "Can't list files in folder, id: " + folderId;
      logger.error(msg, e);
      throw new BoxSDKServiceException(msg, e);
    }
  }

  /**
   * Gets the file content of a {@link BoxFile}.
   *
   * @param file The file to be read
   * @return The file with its content
   * @throws BoxSDKServiceException When an error occurred while getting the file content
   */
  public BoxFileWithContent getFileContent(BoxFile file) throws BoxSDKServiceException {

    BoxFileWithContent boxFileWithContent = new BoxFileWithContent();
    boxFileWithContent.setBoxFile(file);
    boxFileWithContent.setContent(getFileContent(file.getID()));

    return boxFileWithContent;
  }

  /**
   * Gets the file content of a file
   *
   * @param fileId id of the file to be read
   * @return The file content
   * @throws BoxSDKServiceException
   */
  public String getFileContent(String fileId) throws BoxSDKServiceException {
    try {
      BoxFile boxFile = new BoxFile(getBoxAPIConnection(), fileId);
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      boxFile.download(byteArrayOutputStream);
      return byteArrayOutputStream.toString(StandardCharsets.UTF_8.toString());
    } catch (BoxAPIException e) {
      String msg = "Can't get file content, file id: " + fileId;
      logger.error(msg, e);
      throw new BoxSDKServiceException(msg, e);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the HTML code to create an Embed Widget pointing to the given folder
   *
   * @param boxFolder The folder that will be used for the Embed Widget
   * @return The HTML code to add on a page that will display the widget
   */
  public String getBoxEmbedWidgetIFrame(BoxFolder boxFolder) {
    String url = boxFolder.getInfo().getSharedLink().getURL();
    url = url.replaceFirst("/s/", "/embed_widget/s/");
    return "<iframe src=\""
        + url
        + "?view=list&sort=date&theme=blue\" width=\"500\" height=\"400\" show_parent_path=\"yes\" "
        + "frameborder=\"0\" allowfullscreen webkitallowfullscreen mozallowfullscreen oallowfullscreen msallowfullscreen></iframe>";
  }

  /**
   * Get a folder given the folder name from the shared (drop) folder, parent folder. This only
   * searches one level deep.
   *
   * @param folderName The name of the folder to get (not null)
   * @return The searched folder (null if no folder found)
   * @throws BoxSDKServiceException When an error occurred while retrieving the folder
   */
  public BoxFolder getFolderWithName(String folderName) throws BoxSDKServiceException {
    return this.getFolderWithNameAndParentFolderId(
        folderName, getBoxSDKServiceConfig().getRootFolderId());
  }

  /**
   * Get a folder given the folder name from the parent folder. This only searches one level deep.
   *
   * @param folderName The name of the folder to get (not null)
   * @param parentFolderId The parent folder in which to search the folder for
   * @return The searched folder (null if no folder found)
   * @throws BoxSDKServiceException When an error occurred while retrieving the folder
   */
  public BoxFolder getFolderWithNameAndParentFolderId(String folderName, String parentFolderId)
      throws BoxSDKServiceException {

    if (Strings.isNullOrEmpty(folderName)) {
      throw new BoxSDKServiceException("A null folder name is not acceptable");
    }

    BoxFolder folder = null;
    try {
      BoxFolder parentFolder = new BoxFolder(getBoxAPIConnection(), parentFolderId);

      for (BoxItem.Info itemInfo : parentFolder) {
        if (itemInfo instanceof BoxFolder.Info) {
          BoxFolder.Info folderInfo = (BoxFolder.Info) itemInfo;

          if (folderName.equals(folderInfo.getName())) {
            folder = folderInfo.getResource();
            break;
          }
        }
      }
    } catch (BoxAPIException e) {
      String msg = "Error trying to find folder with name = " + folderName;
      logger.error(msg, e);
      throw new BoxSDKServiceException(msg, e);
    }

    return folder;
  }

  /**
   * Delete the given folder and all of its content.
   *
   * @param folderId id of the folder that should be deleted
   * @throws BoxSDKServiceException When an error occurred while deleting the folder
   */
  public void deleteFolderAndItsContent(String folderId) throws BoxSDKServiceException {
    try {
      BoxFolder boxFolder = new BoxFolder(getBoxAPIConnection(), folderId);
      boxFolder.delete(true);
    } catch (BoxAPIException e) {
      String msg = "Error trying to delete folder " + folderId;
      logger.error(msg, e);
      throw new BoxSDKServiceException(msg, e);
    }
  }

  /**
   * Deletes from a folder all content that is older than a given date.
   *
   * @param folderId the folder id that contains the content to be deleted
   * @param olderThan an instant to check against
   * @throws BoxSDKServiceException
   */
  public void deleteFolderContentOlderThan(String folderId, DateTime olderThan)
      throws BoxSDKServiceException {
    try {
      BoxFolder boxFolder = new BoxFolder(getBoxAPIConnection(), folderId);

      for (BoxItem.Info itemInfo : boxFolder) {

        if (itemInfo instanceof BoxFolder.Info) {
          BoxFolder subFolder = (BoxFolder) itemInfo.getResource();

          if (olderThan.isAfter(subFolder.getInfo().getCreatedAt().getTime())) {
            subFolder.delete(true);
          }

        } else if (itemInfo instanceof BoxFile.Info) {

          BoxFile file = (BoxFile) itemInfo.getResource();

          if (olderThan.isAfter(file.getInfo().getCreatedAt().getTime())) {
            file.delete();
          }
        }
      }

    } catch (BoxAPIException e) {
      String msg =
          "Error trying to delete content older than: "
              + olderThan.toString()
              + " in folder: "
              + folderId;
      logger.error(msg, e);
      throw new BoxSDKServiceException(msg, e);
    }
  }

  /**
   * Adds a comment on a file.
   *
   * @param fileId the fileId (not null)
   * @param comment the comment to add on the file
   * @throws BoxSDKServiceException When an error occurred while adding the comment
   */
  public void addCommentToFile(String fileId, String comment) throws BoxSDKServiceException {
    try {
      BoxFile boxFile = new BoxFile(getBoxAPIConnection(), fileId);
      boxFile.addComment(comment);
    } catch (BoxAPIException e) {

      if (isSimilarCommentException(e)) {
        logger.debug("Trying to add the same comment, do nothing");
      } else {
        String msg = "Error trying to add a comment to file " + fileId;
        logger.error(msg, e);
        throw new BoxSDKServiceException(msg, e);
      }
    }
  }

  /**
   * Indicates if an exception is due to trying to add a comment that is the same as the last
   * comment.
   *
   * @param e the exception that contains details about the error
   * @return {@code true} if the exception is due to trying to add a comment that is the same as
   *     last comment else {@code false}
   */
  private boolean isSimilarCommentException(BoxAPIException e) {
    // TODO(P1) would be better to properly parse the response
    return e.getResponseCode() == HttpStatus.CONFLICT.value()
        && e.getResponse().contains("recent_similar_comment");
  }
}
