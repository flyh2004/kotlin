/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.plugin.highlighter;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lexer.JetTokens;

class PropertiesHighlightingVisitor extends AfterAnalysisHighlightingVisitor {
    PropertiesHighlightingVisitor(AnnotationHolder holder, BindingContext bindingContext) {
        super(holder, bindingContext);
    }

    @Override
    public void visitSimpleNameExpression(JetSimpleNameExpression expression) {
        DeclarationDescriptor target = bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
        if (target instanceof VariableAsFunctionDescriptor) {
            target = ((VariableAsFunctionDescriptor)target).getVariableDescriptor();
        }
        if (!(target instanceof PropertyDescriptor)) {
            return;
        }

        if (((PropertyDescriptor)target).getReceiverParameter() != ReceiverDescriptor.NO_RECEIVER) {
            JetPsiChecker.highlightName(holder, expression, JetHighlightingColors.EXTENSION_PROPERTY);
        }

        boolean namespace = target.getContainingDeclaration() instanceof NamespaceDescriptor;
        putPropertyAnnotation(expression, namespace, false);
        if (expression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER) {
            JetPsiChecker.highlightName(holder, expression, JetHighlightingColors.BACKING_FIELD_ACCESS);
        }
    }

    @Override
    public void visitProperty(@NotNull JetProperty property) {
        PsiElement nameIdentifier = property.getNameIdentifier();
        if (nameIdentifier == null) return;
        VariableDescriptor propertyDescriptor = bindingContext.get(BindingContext.VARIABLE, property);
        if (propertyDescriptor instanceof PropertyDescriptor) {
            Boolean backingFieldRequired = bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor)propertyDescriptor);
            boolean namespace = propertyDescriptor.getContainingDeclaration() instanceof NamespaceDescriptor;
            putPropertyAnnotation(nameIdentifier, namespace, Boolean.TRUE.equals(backingFieldRequired));
        }

        super.visitProperty(property);
    }

    @Override
    public void visitParameter(@NotNull JetParameter parameter) {
        PsiElement nameIdentifier = parameter.getNameIdentifier();
        if (nameIdentifier == null) return;
        PropertyDescriptor propertyDescriptor = bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
        if (propertyDescriptor != null) {
            Boolean backingFieldRequired = bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor);
            putPropertyAnnotation(nameIdentifier, false, Boolean.TRUE.equals(backingFieldRequired));
        }
    }

    @Override
    public void visitJetElement(@NotNull JetElement element) {
        element.acceptChildren(this);
    }

    private void putPropertyAnnotation(@NotNull PsiElement elementToHighlight, boolean namespace, boolean withBackingField) {
        JetPsiChecker.highlightName(holder, elementToHighlight,
                                    namespace ? JetHighlightingColors.NAMESPACE_PROPERTY : JetHighlightingColors.INSTANCE_PROPERTY
        );
        if (withBackingField) {
            holder.createInfoAnnotation(
                elementToHighlight,
                "This property has a backing field")
                .setTextAttributes(JetHighlightingColors.PROPERTY_WITH_BACKING_FIELD);
        }
    }
}