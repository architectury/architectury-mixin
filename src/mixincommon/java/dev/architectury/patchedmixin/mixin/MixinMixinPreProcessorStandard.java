/*
 * This file is part of Mixin, licensed under the MIT License (MIT).
 *
 * Copyright (c) architectury, FabricMC, SpongePowered
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package dev.architectury.patchedmixin.mixin;

import me.shedaniel.staticmixin.spongepowered.asm.mixin.Mixin;
import me.shedaniel.staticmixin.spongepowered.asm.mixin.injection.At;
import me.shedaniel.staticmixin.spongepowered.asm.mixin.injection.Coerce;
import me.shedaniel.staticmixin.spongepowered.asm.mixin.injection.Redirect;
import me.shedaniel.staticmixin.spongepowered.asm.mixin.injection.Slice;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

// https://github.com/FabricMC/Mixin/pull/39/
@Mixin(targets = "org.spongepowered.asm.mixin.transformer.MixinPreProcessorStandard")
public class MixinMixinPreProcessorStandard {
    @Redirect(method = "transformMethod", at = @At(value = "INVOKE",
                                                   target = "Lorg/spongepowered/asm/mixin/transformer/ClassInfo;findMethodInHierarchy(Lorg/objectweb/asm/tree/MethodInsnNode;Lorg/spongepowered/asm/mixin/transformer/ClassInfo$SearchType;I)Lorg/spongepowered/asm/mixin/transformer/ClassInfo$Method;"))
    private ClassInfo.Method findMethodInHierarchy(ClassInfo classInfo, MethodInsnNode methodNode, ClassInfo.SearchType searchType, int flags) {
        int includeStatic = (methodNode.getOpcode() == Opcodes.INVOKESTATIC ? ClassInfo.INCLUDE_STATIC : 0);
        ClassInfo.Method method = classInfo.findMethodInHierarchy(methodNode.name, methodNode.desc, searchType,
                ClassInfo.Traversal.NONE, flags | includeStatic);
        
        //Accessors are never renamed, despite it appearing as if they have been
        return method != null && !method.isAccessor() ? method : null;
    }
    
    @Redirect(method = "transformField", at = @At(value = "INVOKE",
                                                  target = "Lorg/spongepowered/asm/mixin/transformer/ClassInfo;findField(Lorg/objectweb/asm/tree/FieldInsnNode;I)Lorg/spongepowered/asm/mixin/transformer/ClassInfo$Field;"))
    private ClassInfo.Field findField(ClassInfo classInfo, FieldInsnNode fieldNode, int flags) {
        int includeStatic = ((fieldNode.getOpcode() == Opcodes.GETSTATIC || fieldNode.getOpcode() == Opcodes.PUTSTATIC) ? ClassInfo.INCLUDE_STATIC : 0);
        return classInfo.findFieldInHierarchy(fieldNode.name, fieldNode.desc, ClassInfo.SearchType.ALL_CLASSES,
                ClassInfo.Traversal.NONE, flags | includeStatic);
    }

    // Fix runtime overwrite remapping
    // https://github.com/FabricMC/Mixin/commit/ec491e3b5d131b657bd095652f0d5195cd33a7cf
    @Redirect(
        method = "attachSpecialMethod",
        at = @At(value = "FIELD", target = "Lorg/spongepowered/asm/mixin/transformer/MixinPreProcessorStandard$SpecialMethod;isOverwrite:Z"),
        slice = @Slice(
            from = @At(value = "INVOKE", target = "Lorg/spongepowered/asm/mixin/transformer/MixinTargetContext;findMethod(Lorg/objectweb/asm/tree/MethodNode;Lorg/objectweb/asm/tree/AnnotationNode;)Lorg/objectweb/asm/tree/MethodNode;"),
            to = @At(value = "INVOKE", target = "Lorg/spongepowered/asm/mixin/transformer/MixinTargetContext;findRemappedMethod(Lorg/objectweb/asm/tree/MethodNode;)Lorg/objectweb/asm/tree/MethodNode;")
        ),
        allow = 1
    )
    private boolean redirectIsOverwrite(@Coerce Object self) {
        // Disable the check that prevents overwrites from getting remapped by
        // pretending the method is not an overwrite
        return false;
    }
}
