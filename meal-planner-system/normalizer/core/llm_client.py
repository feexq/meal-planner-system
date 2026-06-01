from __future__ import annotations

import json
import logging
import os

from google import genai
from google.genai import types
from openai import OpenAI

log = logging.getLogger(__name__)

OPENAI_API_KEY: str = os.environ.get("OPENAI_API_KEY", "")
GEMINI_API_KEY: str = os.environ.get("GEMINI_API_KEY", "")

_openai_client: OpenAI | None = (
    OpenAI(api_key=OPENAI_API_KEY, max_retries=0, timeout=200.0) if OPENAI_API_KEY else None
)
_gemini_client: genai.Client | None = (
    genai.Client(api_key=GEMINI_API_KEY) if GEMINI_API_KEY else None
)

# ── Model fallback chain ─────────────────────────────────────────────────────
_OPENAI_MODELS = [
    "gpt-5-mini",         # primary — cheapest, reasoning
    "gpt-4.1-mini",       # fallback
    "gpt-5.4-nano",       # lightweight fallback
]
_GEMINI_MODELS = [
    "gemini-3-flash-preview",  # 1M ctx, 65K out — last-resort fallback
]


def _strip_md(text: str) -> str:
    text = text.strip()
    if text.startswith("```"):
        lines = text.splitlines()
        end = -1 if lines[-1].strip() == "```" else len(lines)
        text = "\n".join(lines[1:end])
    return text.strip()


def generate_json_with_fallback(
        *,
        system_prompt: str,
        user_prompt: str,
        max_tokens: int = 14_000,
        temperature: float = 0.2,
) -> dict:
    errors: list[str] = []

    # ── OpenAI tiers (primary) ───────────────────────────────────────────────
    if _openai_client:
        for model in _OPENAI_MODELS:
            try:
                log.info("[LLM] Trying OpenAI model=%s", model)
                with open("/tmp/llm_request.txt", "w", encoding="utf-8") as f:
                    f.write(f"=== SYSTEM ===\n{system_prompt}\n\n=== USER ===\n{user_prompt}\n")
                
                resp = _openai_client.chat.completions.create(
                    model=model,
                    messages=[
                        {"role": "system", "content": system_prompt},
                        {"role": "user",   "content": user_prompt},
                    ],
                    response_format={"type": "json_object"},
                )
                
                raw_content = resp.choices[0].message.content or ""
                with open("/tmp/llm_response.txt", "w", encoding="utf-8") as f:
                    f.write(raw_content)
                    
                log.info(f"Raw OpenAI response saved to /tmp/llm_response.txt")
                return json.loads(_strip_md(raw_content))
            except Exception as exc:
                msg = f"OpenAI/{model}: {exc}"
                log.warning("[LLM] %s", msg)
                errors.append(msg)
    else:
        errors.append("OpenAI client not configured (no OPENAI_API_KEY)")

    # ── Gemini fallback ──────────────────────────────────────────────────────
    if _gemini_client:
        for model in _GEMINI_MODELS:
            try:
                log.info("[LLM] Trying Gemini model=%s", model)
                resp = _gemini_client.models.generate_content(
                    model=model,
                    contents=user_prompt,
                    config=types.GenerateContentConfig(
                        system_instruction=system_prompt,
                        temperature=temperature,
                        max_output_tokens=max_tokens,
                        response_mime_type="application/json",
                    ),
                )
                return json.loads(_strip_md(resp.text))
            except Exception as exc:
                msg = f"Gemini/{model}: {exc}"
                log.warning("[LLM] %s", msg)
                errors.append(msg)
    else:
        errors.append("Gemini client not configured (no GEMINI_API_KEY)")

    raise RuntimeError(
        "CRITICAL: All LLM tiers failed.\n" + "\n".join(errors)
    )