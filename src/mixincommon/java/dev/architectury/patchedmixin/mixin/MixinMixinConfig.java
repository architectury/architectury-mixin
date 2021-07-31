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

import dev.architectury.patchedmixin.refmap.IClassReferenceMapper;
import me.shedaniel.staticmixin.spongepowered.asm.mixin.Mixin;
import me.shedaniel.staticmixin.spongepowered.asm.mixin.Overwrite;
import me.shedaniel.staticmixin.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;

@Mixin(targets = "org.spongepowered.asm.mixin.transformer.MixinConfig")
public abstract class MixinMixinConfig {
    @Shadow
    public abstract IReferenceMapper getReferenceMapper();
    
    @Overwrite
    String remapClassName(String className, String reference) {
        IReferenceMapper mapper = this.getReferenceMapper();
        if (mapper instanceof IClassReferenceMapper) {
            return ((IClassReferenceMapper) mapper).arch$remapClassName(className, reference);
        } else {
            return mapper.remap(className, reference);
        }
    }
}
