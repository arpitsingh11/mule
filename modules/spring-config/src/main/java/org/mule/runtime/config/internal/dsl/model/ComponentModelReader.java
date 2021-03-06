/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.model.config.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.dsl.api.xml.parser.ConfigLine;
import org.mule.runtime.dsl.api.xml.parser.SimpleConfigAttribute;
import org.mule.runtime.internal.dsl.DslConstants;

import java.util.List;
import java.util.Properties;

/**
 * Class used to read xml files from {@link ConfigLine}s, unifying knowledge on how to properly read the files returning the
 * {@link ComponentModel} object.
 *
 * It also replaces the values of the attributes by using the {@link Properties} object parametrized in its constructor.
 */
public class ComponentModelReader {

  private final ConfigurationPropertiesResolver configurationPropertiesResolver;

  public ComponentModelReader(ConfigurationPropertiesResolver configurationPropertiesResolver) {
    this.configurationPropertiesResolver = configurationPropertiesResolver;
  }

  public ComponentAst extractComponentDefinitionModel(ConfigLine configLine, String configFileName) {

    String namespace = configLine.getNamespace() == null ? CORE_PREFIX : configLine.getNamespace();
    String namespaceUri = configLine.getNamespaceUri() == null ? DslConstants.CORE_NAMESPACE : configLine.getNamespaceUri();
    ComponentModel.Builder builder = new ComponentModel.Builder()
        .setIdentifier(builder()
            .namespace(namespace)
            .namespaceUri(namespaceUri)
            .name(configLine.getIdentifier())
            .build())
        .setTextContent(resolveValueIfIsPlaceHolder(configLine.getTextContent()))
        .setConfigFileName(configFileName)
        .setLineNumber(configLine.getLineNumber())
        .setStartColumn(configLine.getStartColumn())
        .setSourceCode(configLine.getSourceCode());

    configLine.getCustomAttributes()
        .forEach((key, value) -> {
          builder.addCustomAttribute(key, value);
        });

    for (SimpleConfigAttribute simpleConfigAttribute : configLine.getConfigAttributes().values()) {
      builder.addParameter(simpleConfigAttribute.getName(), resolveValueIfIsPlaceHolder(simpleConfigAttribute.getValue()),
                           simpleConfigAttribute.isValueFromSchema());
    }

    List<ComponentAst> componentModels = configLine.getChildren().stream()
        .map(childConfigLine -> extractComponentDefinitionModel(childConfigLine, configFileName))
        .collect(toList());
    componentModels.stream()
        .forEach(componentDefinitionModel -> builder.addChildComponentModel((ComponentModel) componentDefinitionModel));
    return builder.build();
  }

  private String resolveValueIfIsPlaceHolder(String value) {
    Object resolvedValue = configurationPropertiesResolver.resolveValue(value);
    return resolvedValue instanceof String ? (String) resolvedValue : (resolvedValue != null ? resolvedValue.toString() : null);
  }

}
