import sys
import os
import json
import torch

# Suppress noisy transformer warnings before imports
os.environ["TRANSFORMERS_VERBOSITY"] = "error"

from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import PeftModel
from json_reader import get_volunteer_field

# Get absolute path of the engine directory and project root
ENGINE_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(ENGINE_DIR, "../../../"))
ADAPTER_DIR = os.path.join(ENGINE_DIR, "slm_lora_adapter")
STORAGE_DIR = os.path.join(PROJECT_ROOT, "json_data_storage")

BASE_MODEL = "Qwen/Qwen2.5-0.5B-Instruct"

def load_inference_model():
    tokenizer = AutoTokenizer.from_pretrained(BASE_MODEL, trust_remote_code=True)

    device = "cuda" if torch.cuda.is_available() else "cpu"
    torch_dtype = torch.float16 if torch.cuda.is_available() else torch.float32

    base_model = AutoModelForCausalLM.from_pretrained(
        BASE_MODEL,
        torch_dtype=torch_dtype,
        device_map="auto" if torch.cuda.is_available() else None,
        trust_remote_code=True
    )

    if os.path.exists(ADAPTER_DIR):
        model = PeftModel.from_pretrained(base_model, ADAPTER_DIR)
    else:
        model = base_model

    return model, tokenizer

def run_inference(user_prompt):
    model, tokenizer = load_inference_model()

    messages = [
        {
            "role": "system",
            "content": (
                "You are an AI assistant for the AYLUS volunteer management system. "
                "Output strictly a JSON object with the format: "
                "{\"action\": \"get_volunteer_data\", \"parameters\": {\"name\": \"...\", \"branch\": \"...\", \"field\": \"...\"}}."
            )
        },
        {"role": "user", "content": user_prompt}
    ]

    prompt = tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
    inputs = tokenizer(prompt, return_tensors="pt").to(model.device)

    with torch.no_grad():
        outputs = model.generate(
            **inputs,
            max_new_tokens=128,
            temperature=0.01,
            do_sample=False
        )

    decoded = tokenizer.decode(outputs[0], skip_special_tokens=True)

    try:
        assistant_text = decoded.split("assistant")[-1].strip()
        start_idx = assistant_text.find("{")
        end_idx = assistant_text.rfind("}") + 1
        json_str = assistant_text[start_idx:end_idx]

        parsed_call = json.loads(json_str)
        params = parsed_call.get("parameters", {})
        name = params.get("name")
        branch = params.get("branch")
        field = params.get("field", "totalHours")

        # Normalize field names to match json_data_storage keys (e.g., hours -> totalHours)
        if field.lower() in ["hour", "hours", "totalhours"]:
            field = "totalHours"
        elif field.lower() in ["money", "owed", "payment"]:
            field = "totalMoneyOwed"

        result = get_volunteer_field(name, branch, field, json_dir=STORAGE_DIR)

        print(f"{name} has {result} for '{field}' in the {branch} branch.")

    except Exception as e:
        print(f"Error parsing model tool call: {str(e)}")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        query_text = sys.argv[1]
        run_inference(query_text)
    else:
        print("Error: No query argument provided.")