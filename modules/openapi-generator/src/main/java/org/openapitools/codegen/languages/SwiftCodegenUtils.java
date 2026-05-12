/*
 * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 * Copyright 2018 SmartBear Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openapitools.codegen.languages;

import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class SwiftCodegenUtils {

    static final String X_SWIFT_SHARED_ACCESSORS = "x-swift-shared-accessors";

    private static final Logger LOGGER = LoggerFactory.getLogger(SwiftCodegenUtils.class);

    private SwiftCodegenUtils() {}

    // A model declared as `type: object` with both sibling properties and `oneOf`
    // is rendered as a Swift enum with no stored properties. Without this pass
    // the parent's sibling properties are silently dropped from the output. Here
    // we surface properties that are present (with matching types) on every
    // variant as computed accessors on the enum.
    static void attachSharedAccessors(Map<String, ModelsMap> objs) {
        for (Map.Entry<String, ModelsMap> entry : objs.entrySet()) {
            CodegenModel cm = firstModel(entry.getValue());
            if (cm == null) continue;
            if (!Boolean.TRUE.equals(cm.vendorExtensions.get(CodegenConstants.X_IS_ONE_OF_INTERFACE))) continue;
            if (cm.vars == null || cm.vars.isEmpty()) continue;
            if (cm.oneOf == null || cm.oneOf.isEmpty()) continue;

            List<Map<String, CodegenProperty>> variantProperties = resolveVariantProperties(cm, objs);
            if (variantProperties == null) continue;

            List<CodegenProperty> shared = new ArrayList<>();
            for (CodegenProperty parentProp : cm.vars) {
                if (isSharedAcrossVariants(parentProp, variantProperties)) {
                    shared.add(parentProp);
                }
            }
            if (!shared.isEmpty()) {
                cm.vendorExtensions.put(X_SWIFT_SHARED_ACCESSORS, shared);
            }
        }
    }

    private static List<Map<String, CodegenProperty>> resolveVariantProperties(CodegenModel cm, Map<String, ModelsMap> objs) {
        List<Map<String, CodegenProperty>> result = new ArrayList<>(cm.oneOf.size());
        for (String variantName : cm.oneOf) {
            CodegenModel variant = ModelUtils.getModelByName(variantName, objs);
            if (variant == null) {
                LOGGER.info("Cannot resolve oneOf variant '{}' for '{}'; skipping shared-accessor pass.", variantName, cm.name);
                return null;
            }
            result.add(indexByBaseName(variant));
        }
        return result;
    }

    private static Map<String, CodegenProperty> indexByBaseName(CodegenModel variant) {
        List<CodegenProperty> source = variant.allVars != null && !variant.allVars.isEmpty()
                ? variant.allVars
                : variant.vars;
        if (source == null) return Collections.emptyMap();
        Map<String, CodegenProperty> index = new HashMap<>(source.size());
        for (CodegenProperty p : source) {
            index.putIfAbsent(p.baseName, p);
        }
        return index;
    }

    private static boolean isSharedAcrossVariants(CodegenProperty parentProp, List<Map<String, CodegenProperty>> variantProperties) {
        CodegenProperty first = null;
        for (Map<String, CodegenProperty> index : variantProperties) {
            CodegenProperty match = index.get(parentProp.baseName);
            if (match == null) return false;
            if (first == null) {
                first = match;
            } else if (!Objects.equals(first.dataType, match.dataType)
                    || !Objects.equals(first.datatypeWithEnum, match.datatypeWithEnum)
                    || first.required != match.required
                    || first.isNullable != match.isNullable) {
                return false;
            }
        }
        return first != null;
    }

    private static CodegenModel firstModel(ModelsMap mm) {
        if (mm == null) return null;
        List<ModelMap> list = mm.getModels();
        if (list == null) return null;
        for (ModelMap m : list) {
            CodegenModel model = m.getModel();
            if (model != null) return model;
        }
        return null;
    }
}
