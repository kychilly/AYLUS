import torch
from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import LoraConfig, get_peft_model, TaskType

# Base model choice (under 500M parameters)
MODEL_NAME = "Qwen/Qwen2.5-0.5B-Instruct"

def get_lora_model_and_tokenizer(model_name=MODEL_NAME):
    """
    Loads base pre-trained model and wraps attention layers with LoRA adapters.
    Freezes base parameters while keeping target projection layers trainable.
    """
    print(f"Loading base pre-trained model: '{model_name}'...")

    # 1. Load Tokenizer
    tokenizer = AutoTokenizer.from_pretrained(model_name, trust_remote_code=True)
    if tokenizer.pad_token is None:
        tokenizer.pad_token = tokenizer.eos_token
        tokenizer.pad_token_id = tokenizer.eos_token_id
    tokenizer.padding_side = "right"

    # 2. Load Base PyTorch Causal LM
    device_map = "auto" if torch.cuda.is_available() else None
    torch_dtype = torch.float16 if torch.cuda.is_available() else torch.float32

    base_model = AutoModelForCausalLM.from_pretrained(
        model_name,
        torch_dtype=torch_dtype,
        device_map=device_map,
        trust_remote_code=True
    )

    # 3. Define LoRA Configuration targeting Attention Projections
    lora_config = LoraConfig(
        r=16,                         # Rank dimension (controls trainable capacity)
        lora_alpha=32,                # Scaling factor
        target_modules=["q_proj", "v_proj", "k_proj", "o_proj"], # Target attention layers
        lora_dropout=0.05,            # Dropout for regularization
        bias="none",
        task_type=TaskType.CAUSAL_LM
    )

    # 4. Wrap Base Model with PEFT Adapter
    peft_model = get_peft_model(base_model, lora_config)

    # 5. Print Trainable vs. Frozen Parameter Metrics
    peft_model.print_trainable_parameters()

    return peft_model, tokenizer

if __name__ == "__main__":
    model, tokenizer = get_lora_model_and_tokenizer()
    print("\nLoRA adapter successfully attached to base model architecture!")