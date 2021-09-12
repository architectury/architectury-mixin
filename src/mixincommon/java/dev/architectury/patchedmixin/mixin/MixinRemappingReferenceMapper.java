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

import dev.architectury.patchedmixin.base.RemappingReferenceMapperBase;
import dev.architectury.patchedmixin.refmap.IClassReferenceMapper;
import me.shedaniel.staticmixin.spongepowered.asm.mixin.*;
import me.shedaniel.staticmixin.spongepowered.asm.mixin.injection.At;
import me.shedaniel.staticmixin.spongepowered.asm.mixin.injection.Inject;
import me.shedaniel.staticmixin.spongepowered.asm.mixin.injection.Redirect;
import me.shedaniel.staticmixin.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IRemapper;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;
import org.spongepowered.asm.mixin.refmap.RemappingReferenceMapper;

import java.util.HashMap;
import java.util.Map;

@Mixin(RemappingReferenceMapper.class)
public abstract class MixinRemappingReferenceMapper implements IClassReferenceMapper, RemappingReferenceMapperBase {
    @Unique
    private IRemapper arch$remapper;
    
    @Shadow @Final
    private IReferenceMapper refMap;
    
    @Unique
    private final Map<String, String> arch$mappedReferenceCache = new HashMap<>();
    
    @Inject(method = "<init>", at = @At("RETURN"))
    public void arch$init(MixinEnvironment env, IReferenceMapper refMap, CallbackInfo info) {
        arch$remapper = env.getRemappers();
    }
    
    @Overwrite
    public void setContext(String context) {
        this.refMap.setContext(context);
    }
    
    @Overwrite
    public String remap(String className, String reference) {
        return ((RemappingReferenceMapper) (Object) this).remapWithContext(((RemappingReferenceMapper) (Object) this).getContext(), className, reference);
    }
    
    @Overwrite
    public String remapWithContext(String context, String className, String reference) {
        if (reference.isEmpty()) {
            return reference;
        }
        String origInfoString = this.refMap.remapWithContext(context, className, reference);
        String remappedCached = arch$mappedReferenceCache.get(origInfoString);
        if (remappedCached != null) {
            return remappedCached;
        } else {
            String remapped = origInfoString;
            
            // To handle propagation, find super/itf-class (for IRemapper)
            // but pass the requested class in the MemberInfo
            MemberInfo info = MemberInfo.parse(remapped, null);
            if (info.getName() == null && info.getDesc() == null) {
                return info.getOwner() != null ? arch$newMemberInfo(arch$remapper.map(info.getOwner())).toString() : info.toString();
            } else if (info.isField()) {
                remapped = new MemberInfo(
                        arch$remapper.mapFieldName(info.getOwner(), info.getName(), info.getDesc()),
                        info.getOwner() == null ? null : arch$remapper.map(info.getOwner()),
                        info.getDesc() == null ? null : arch$remapper.mapDesc(info.getDesc())
                ).toString();
            } else {
                remapped = new MemberInfo(
                        arch$remapper.mapMethodName(info.getOwner(), info.getName(), info.getDesc()),
                        info.getOwner() == null ? null : arch$remapper.map(info.getOwner()),
                        info.getDesc() == null ? null : arch$remapMethodDescriptor(arch$remapper, info.getDesc())
                ).toString();
            }
            arch$mappedReferenceCache.put(origInfoString, remapped);
            return remapped;
        }
    }
    
    @Unique
    private static String arch$remapMethodDescriptor(IRemapper remapper, String desc) {
        StringBuilder newDesc = new StringBuilder();
        newDesc.append('(');
        for (Type arg : Type.getArgumentTypes(desc)) {
            newDesc.append(remapper.mapDesc(arg.getDescriptor()));
        }
        return newDesc.append(')').append(remapper.mapDesc(Type.getReturnType(desc).getDescriptor())).toString();
    }
    
    @Redirect(method = "of", at = @At(value = "INVOKE",
                                      target = "Lorg/spongepowered/asm/mixin/refmap/RemappingReferenceMapper;hasData(Lorg/spongepowered/asm/mixin/MixinEnvironment;)Z"))
    private static boolean arch$dontCheckHasData(MixinEnvironment environment) {
        return true;
    }
    
    @Override
    public String arch$remapClassName(String className, String inputClassName) {
        return arch$remapClassNameWithContext(((RemappingReferenceMapper) (Object) this).getContext(), className, inputClassName);
    }
    
    @Override
    public String arch$remapClassNameWithContext(String context, String className, String remapped) {
        String origInfoString;
        if (this.refMap instanceof IClassReferenceMapper) {
            origInfoString = ((IClassReferenceMapper) this.refMap).arch$remapClassNameWithContext(context, className, remapped);
        } else {
            origInfoString = this.refMap.remapWithContext(context, className, remapped);
        }
        return arch$remapper.map(origInfoString.replace('.', '/'));
    }
}
