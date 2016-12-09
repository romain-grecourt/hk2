package org.jvnet.hk2.osgiadapter;

import com.sun.enterprise.module.Repository;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import static org.jvnet.hk2.osgiadapter.OSGiJarBasedRepository.isJarUri;
import static org.jvnet.hk2.osgiadapter.Logger.logger;

/**
 *
 * @author rgrecour
 */
public class OSGiRepositoryFactory {

    private static OSGiRepositoryFactory instance;

    public static synchronized void initialize(URI repo) {
        if (instance != null) {
            // TODO : this is somehow invoked twice during gf startup, we need to investigate.
            logger.logp(Level.FINE, "OSGiFactoryImpl", "initialize",
                    "Singleton already initialized as {0}", getInstance());
        }
        instance = new OSGiRepositoryFactory(repo);
    }

    public static OSGiRepositoryFactory getInstance() {
        assert(instance != null);
        return instance;
    }

    private final URI repoUri;

    private OSGiRepositoryFactory(URI repo){
        this.repoUri = repo;
    }

    public Repository createRepository() throws IOException {
        Repository repo;
        if (isJarUri(repoUri)) {
            repo = new OSGiJarBasedRepository(repoUri);
        } else {
            File repoDir = new File(repoUri);
            repo = new OSGiDirectoryBasedRepository(repoDir.getAbsolutePath(), repoDir);
        }
        repo.initialize();
        return repo;
    }
}
