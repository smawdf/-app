# -*- coding: utf-8 -*-
import re, glob, os

# 1. Collect all Chinese chars + common punctuation from OrderDisk source
chars = set()
for f in glob.glob(r'app/src/main/java/**/*.kt', recursive=True):
    try:
        t = open(f, encoding='utf-8', errors='ignore').read()
    except Exception:
        continue
    for m in re.findall(r'"([^"\\]{1,200})"', t):
        for c in m:
            if '\u4e00' <= c <= '\u9fff':
                chars.add(c)
            elif c in '，。！？：；、（）《》【】—…·￥%&*+-=@#_.,:;!?()[]{}<>~^|/\\\'\u201c\u201d ':
                chars.add(c)
for c in '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ元角分份斤两碗杯碟':
    chars.add(c)
print('collected chars:', len(chars))
chars = sorted(chars)

# 2. Subset 浪漫雅圆GB.ttf
from fontTools import subset
src = 'preview-fonts-%E6%B5%AA%E6%BC%AB%E9%9B%85%E5%9C%86GB.ttf'
opts = subset.Options()
opts.flavor = None
opts.layout_features = ['*']
opts.notdef_outline = True
opts.name_IDs = [1, 2, 3, 4, 6]
sub = subset.Subsetter(options=opts)
sub.populate(text=''.join(chars))
font = subset.load_font(src, opts)
sub.subset(font)
subset.save_font(font, 'preview-fonts-romantic-round-subset.ttf', opts)
print('subset done: %d KB -> %d KB' % (
    os.path.getsize(src) // 1024,
    os.path.getsize('preview-fonts-romantic-round-subset.ttf') // 1024))
