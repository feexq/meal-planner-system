from ddgs import DDGS
ddgs = DDGS()
results = list(ddgs.images("almond milk product packaging", max_results=5))
print(len(results), results[:2] if results else "ПОРОЖНЬО")