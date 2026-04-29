from __future__ import annotations

import json
import logging
import os

from google import genai
from google.genai import types
from openai import OpenAI

log = logging.getLogger(__name__)

GEMINI_API_KEY: str = os.environ.get("GEMINI_API_KEY", "")
GROQ_API_KEY: str   = os.environ.get("GROQ_API_KEY", "")

_gemini_client: genai.Client | None = (
    genai.Client(api_key=GEMINI_API_KEY) if GEMINI_API_KEY else None
)
_groq_client: OpenAI | None = (
    OpenAI(api_key=GROQ_API_KEY, base_url="https://api.groq.com/openai/v1")
    if GROQ_API_KEY
    else None
)

_GEMINI_MODELS = [
    "gemini-3.1-flash-lite-preview",
    "gemini-3-flash-preview",
]
_GROQ_MODELS = [
    "llama-3.3-70b-versatile",
    "llama-3.1-8b-instant",
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
        fallback_system_prompt: str | None = None,
        fallback_user_prompt: str | None = None,
        max_tokens: int = 14_000,
        temperature: float = 0.2,
) -> dict:
    groq_sys  = fallback_system_prompt or system_prompt
    groq_user = fallback_user_prompt   or user_prompt

    errors: list[str] = []

    # ── Gemini tiers ──────────────────────────────────────────────────────────
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

    # ── Groq tiers ────────────────────────────────────────────────────────────
    if _groq_client:
        for model in _GROQ_MODELS:
            try:
                log.info("[LLM] Trying Groq model=%s", model)
                resp = _groq_client.chat.completions.create(
                    model=model,
                    messages=[
                        {"role": "system", "content": groq_sys},
                        {"role": "user",   "content": groq_user},
                    ],
                    response_format={"type": "json_object"},
                    temperature=temperature,
                )
                return json.loads(_strip_md(resp.choices[0].message.content))
            except Exception as exc:
                msg = f"Groq/{model}: {exc}"
                log.warning("[LLM] %s", msg)
                errors.append(msg)
    else:
        errors.append("Groq client not configured (no GROQ_API_KEY)")

    raise RuntimeError(
        "CRITICAL: All LLM tiers failed.\n" + "\n".join(errors)
    )