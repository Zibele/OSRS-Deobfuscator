/*
 * Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package deob.deobfuscators.mapping;

import asm.ClassFile;
import asm.ClassGroup;
import asm.Field;
import asm.Method;
import asm.attributes.Annotations;
import asm.attributes.annotation.Annotation;
import asm.attributes.annotation.Element;
import deob.DeobAnnotations;

public class AnnotationMapper
{

	private final ClassGroup source, target;
	private final ParallelExecutorMapping mapping;

	public AnnotationMapper(ClassGroup source, ClassGroup target, ParallelExecutorMapping mapping)
	{
		this.source = source;
		this.target = target;
		this.mapping = mapping;
	}

	public void run()
	{
		int count = 0;

		for (ClassFile c : source.getClasses())
		{
			ClassFile other = (ClassFile) mapping.get(c);

			count += run(c, other);
		}

	}

	private int run(ClassFile from, ClassFile to)
	{
		int count = 0;

		if (hasCopyableAnnotation(from.getAnnotations()))
		{
			if (to != null)
			{
				count += copyAnnotations(from.getAnnotations(), to.getAnnotations());
			}
		}

		for (Field f : from.getFields())
		{
			if (!hasCopyableAnnotation(f.getAnnotations()))
				continue;

			Field other = (Field) mapping.get(f);
			if (other == null)
			{
				continue;
			}

			count += copyAnnotations(f.getAnnotations(), other.getAnnotations());
		}

		for (Method m : from.getMethods())
		{
			if (!hasCopyableAnnotation(m.getAnnotations()))
				continue;

			Method other = (Method) mapping.get(m);
			if (other == null)
			{
				continue;
			}

			count += copyAnnotations(m.getAnnotations(), other.getAnnotations());
		}

		return count;
	}

	private int copyAnnotations(Annotations from, Annotations to)
	{
		int count = 0;

		if (from.getAnnotations() == null)
			return count;
		
		for (Annotation a : from.getAnnotations())
		{
			if (isCopyable(a))
			{
				Annotation annotation = new Annotation(to);
				annotation.setType(a.getType());
				to.addAnnotation(annotation);

				for (Element e : a.getElements())
				{
					Element element = new Element(annotation);
					element.setName(e.getName());
					element.setValue(e.getValue());
					annotation.addElement(element);
				}

				++count;
			}
		}

		return count;
	}

	private boolean hasCopyableAnnotation(Annotations a)
	{
		for (Annotation an : a.getAnnotations())
			if (isCopyable(an))
				return true;

		return false;
	}

	private boolean isCopyable(Annotation a)
	{
		return a.getType().equals(DeobAnnotations.EXPORT)
			|| a.getType().equals(DeobAnnotations.IMPLEMENTS)
			|| a.getType().equals(DeobAnnotations.HOOK);
	}
}
