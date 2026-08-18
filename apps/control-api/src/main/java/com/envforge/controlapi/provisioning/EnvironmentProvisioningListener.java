package com.envforge.controlapi.provisioning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EnvironmentProvisioningListener {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(
            EnvironmentProvisioningListener.class
        );

    private final EnvironmentProvisioningService
        environmentProvisioningService;

    public EnvironmentProvisioningListener(
        EnvironmentProvisioningService
            environmentProvisioningService
    ) {
        this.environmentProvisioningService =
            environmentProvisioningService;
    }

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(
        EnvironmentRequestedEvent event
    ) {
        LOGGER.info(
            "Processing provisioning request for environment {}",
            event.environmentId()
        );

        environmentProvisioningService.provision(
            event.environmentId()
        );
    }
}