/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.amazon.aws.spinnaker.plugin.lambda.traffic;

import com.amazon.aws.spinnaker.plugin.lambda.LambdaStageBaseTask;
import com.amazon.aws.spinnaker.plugin.lambda.traffic.model.LambdaTrafficUpdateInput;
import com.amazon.aws.spinnaker.plugin.lambda.utils.LambdaCloudDriverUtils;
import com.amazon.aws.spinnaker.plugin.lambda.utils.LambdaDefinition;
import com.amazon.aws.spinnaker.plugin.lambda.verify.model.LambdaCloudDriverTaskResults;
import com.amazonaws.services.lambda.model.AliasConfiguration;
import com.amazonaws.services.lambda.model.AliasRoutingConfiguration;
import com.netflix.spinnaker.orca.api.pipeline.TaskResult;
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import com.netflix.spinnaker.orca.clouddriver.config.CloudDriverConfigurationProperties;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LambdaTrafficUpdateVerificationTask implements LambdaStageBaseTask {

    private static final Logger logger = LoggerFactory.getLogger(LambdaTrafficUpdateVerificationTask.class);

    @Autowired
    CloudDriverConfigurationProperties props;

    @Autowired
    private LambdaCloudDriverUtils utils;

    @SneakyThrows
    @NotNull
    @Override
    public TaskResult execute(@NotNull StageExecution stage) {
        prepareTask(stage);
        Map<String, Object> stageContext = stage.getContext();
        String url = (String)stageContext.get("url");
        if (url == null) {
            return formErrorTaskResult(stage, String.format("No task url to verify"));
        }

        LambdaCloudDriverTaskResults op = utils.verifyStatus(url);

        if (!op.getStatus().isCompleted()) {
            return TaskResult.builder(ExecutionStatus.RUNNING).build();
        }

        if (op.getStatus().isFailed()) {
            ExecutionStatus status = ExecutionStatus.TERMINAL;
            return formErrorTaskResult(stage,op.getErrors().getMessage());
        }

        if (!"$WEIGHTED".equals(stage.getContext().get("deploymentStrategy"))) {
            boolean valid = validateWeights(stage);
            if (!valid) {
                formErrorTaskResult(stage, "Could not update weights in time");
                return TaskResult.builder(ExecutionStatus.TERMINAL).outputs(stage.getOutputs()).build();
            }
        }

        copyContextToOutput(stage);
        return taskComplete(stage);
    }

    private boolean validateWeights(StageExecution stage) throws InterruptedException {
        AliasRoutingConfiguration weights = null;
        long startTime = System.currentTimeMillis();
        LambdaTrafficUpdateInput inp = utils.getInput(stage, LambdaTrafficUpdateInput.class);
        boolean status = true;
        do {
            System.out.println("while");
            LambdaDefinition lf = utils.retrieveLambdaFromCache(stage, false);
            Thread.sleep(3000);
            Optional<AliasConfiguration> aliasConfiguration = lf.getAliasConfigurations().stream().filter(al -> al.getName().equals(inp.getAliasName())).findFirst();

            if (aliasConfiguration.isPresent()) {
                Optional<AliasRoutingConfiguration> opt = Optional.ofNullable(aliasConfiguration.get().getRoutingConfig());
                weights = opt.orElse(null);
            }
            logger.info("lambdaaaaa: {}",lf);
            if ((System.currentTimeMillis()-startTime)>240000) {
                status = false;
            }
        } while (null != weights && status);
        System.out.println("sali");
        return status;
    }
}
