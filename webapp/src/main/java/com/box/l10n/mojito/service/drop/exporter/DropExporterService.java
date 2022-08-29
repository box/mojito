package com.box.l10n.mojito.service.drop.exporter;

import com.box.l10n.mojito.entity.Drop;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.drop.DropRepository;
import com.box.l10n.mojito.service.pollableTask.ParentTask;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to create {@link DropExporter}s.
 *
 * @author jaurambault
 */
@Service
public class DropExporterService {

  static Logger logger = LoggerFactory.getLogger(DropExporterService.class);

  @Autowired DropRepository dropRepository;

  /**
   * Creates and initializes a {@link DropExporter} for a {@link Drop}. Gets the {@link
   * DropExporterType} from {@link Repository} and update the {@link Drop} entity with the
   * DropExporter configuration.
   *
   * @param drop
   * @param parentTask
   * @return
   */
  @Pollable(message = "Create and prepare the Drop exporter")
  public DropExporter createDropExporterAndUpdateDrop(
      Drop drop, @ParentTask PollableTask parentTask)
      throws DropExporterException, DropExporterInstantiationException {

    Repository repository = drop.getRepository();

    logger.debug("Get the drop exporter based for repository id: {}", repository.getId());
    DropExporter dropExporter =
        createDropExporterInstance(repository.getDropExporterType().getClassName());
    dropExporter.init(repository.getName(), drop.getName());

    logger.debug("Update the drop entity with dropExporter configuration");
    drop.setDropExporterConfig(dropExporter.getConfig());
    drop.setDropExporterType(dropExporter.getDropExporterType());
    dropRepository.save(drop);

    return dropExporter;
  }

  /**
   * Re-creates a {@link DropExporter} for a {@link Drop}.
   *
   * @param drop
   * @return
   */
  public DropExporter recreateDropExporter(Drop drop) throws DropExporterException {

    logger.debug("Get the drop exporter based for an existing drop id: {}", drop.getId());
    DropExporter dropExporter =
        createDropExporterInstance(drop.getDropExporterType().getClassName());

    logger.debug("Set the config coming from the Drop and initialize the dropExporter");
    dropExporter.setConfig(drop.getDropExporterConfig());
    dropExporter.init(drop.getRepository().getName(), drop.getName());

    return dropExporter;
  }

  /**
   * Creates a {@link DropExporter} instance using reflection for a given class name.
   *
   * @param className class name used to create the {@link DropExporter} via reflexion
   * @return an instance of
   * @throws DropExporterInstantiationException if the {@link DropExporter} couldn't be created
   */
  private DropExporter createDropExporterInstance(String className)
      throws DropExporterInstantiationException {

    DropExporter dropExporter;

    try {
      Class<?> clazz = Class.forName(className);
      dropExporter = (DropExporter) clazz.newInstance();
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      throw new DropExporterInstantiationException(
          "Cannot create an instance of DropExporter using reflexion", e);
    }

    return dropExporter;
  }
}
