<script>
    import { browser } from "$app/environment";
    import { page } from "$app/stores";
    let input, errorMessage, file;

    const setError = message => errorMessage = message;

    function onChange() {
        file = input.files[0];
        file.type === "application/zip"
            ? setError(null)
            : setError("The file must be a zip!");
    }

    async function onConfirm() {
        const formData = new FormData();
        formData.append("pack", file);

        const res = await fetch(`${import.meta.env.VITE_API_URL}/uploadpack?token=${$page?.data?.token}`, {
            method: "POST",
            body: formData
        }).then(e => e.json()).catch(e => setError(e));

        if(browser && res.success) window.location.href = "/uploaded";
    }
</script>
<svelte:head>
    <title>Pack Per Region Uploader</title>
</svelte:head>
{#if $page?.data?.uuid && $page?.data?.token}
    <h1>Upload Resource Pack</h1>
    <div id="content">
        <p>Uploading as 
            {#await fetch(`https://playerdb.co/api/player/minecraft/${$page?.data?.uuid}`).then(e => e.json())}
                <span>...</span>
            {:then res}
                <span>{res.data.player.username}</span>
            {:catch error}
                {setError(error.message)}
            {/await}
        </p>
        <input type="file" bind:this={input} on:change={onChange} />
        {#if errorMessage}
            <span class="error">{errorMessage}</span>
        {/if}
        <button on:click={() => onConfirm()} class={file && !errorMessage ? "" : "disabled"} disabled="{!(file && !errorMessage)}">Confirm Upload</button>
    </div>
{:else}
    <div id="content">
        <span class="error">You must access this page through PackPerRegion's /registerarea command!</span>
    </div>
{/if}