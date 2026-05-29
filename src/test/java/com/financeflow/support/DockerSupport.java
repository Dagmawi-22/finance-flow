package com.financeflow.support;

import org.testcontainers.DockerClientFactory;

public final class DockerSupport {

    private DockerSupport() {
    }

    public static boolean isAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }
}
